package org.ipan.nrgyrent.telegram.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.exception.WalletAlreadyHasActiveSessionException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoTopupSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.ipan.nrgyrent.tron.node.events.TronZeroMqListener;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Collections;
import java.util.List;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class AutoDelegationHandler {
    private final AutoDelegationViews autoDelegationViews;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final EnergyService energyService;
    private final UserService userService;
    private final UserWalletService userWalletService;

    @MatchStates({
            @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.AUTOTOPUP),
    })
    public void handleAdminMenu(UserState userState, Update update) {
        List<WalletWithAutoTopupSession> walletsWithSessions = autoDelegationSessionRepo.findActiveSessionsWithWalletInfo(userState.getTelegramId());
        autoDelegationViews.autoDelegationMenu(userState, walletsWithSessions);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTOTOPUP));
    }

    @MatchState(state = States.AUTOTOPUP, updateTypes = UpdateType.CALLBACK_QUERY)
    public void toggleAutoTopupSession(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        InlineMenuCallbacks.ToggleWalletSessionPayload toggleWalletSession = InlineMenuCallbacks.getToggleWalletSession(data);

        if (toggleWalletSession != null) {
            if (toggleWalletSession.getSessionId() != null) {
                // If sessionId is not null that means that there is an existing session and user wants to terminate it.
                energyService.deactivateSessionManually(toggleWalletSession.getSessionId());
                // render menu again
                handleAdminMenu(userState, update);
            } else if (toggleWalletSession.getAddress() != null) {
                // If wallet is not null that means that it is a request to start a session.
                try {
                    if (!TronZeroMqListener.isConnected.get()) {
                        autoDelegationViews.nodeIsUnavailableRightNow(userState);
                        return;
                    }

                    AutoDelegationSession newSession = energyService.startAutoTopupSession(userState, toggleWalletSession.getAddress());
                    // send new menu message, use old one for status updates.
                    autoDelegationViews.updateSessionStatus(userState, newSession);
                    AppUser user = userService.getById(userState.getTelegramId());
                    List<UserWallet> userWallets = Collections.emptyList();
                    if (user.getShowWalletsMenu()) {
                        userWallets = userWalletService.getWallets(user.getTelegramId());
                    }
                    Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
                    telegramState.updateUserState(userState.getTelegramId(), userState
                            .withState(States.MAIN_MENU)
                            .withChatId(newMenuMsg.getChatId())
                            .withMenuMessageId(newMenuMsg.getMessageId()));

                    energyService.tryMakeFirstAutoTopupAsync(newSession.getId());
                } catch (WalletAlreadyHasActiveSessionException e) {
                    autoDelegationViews.walletIsAlreadyHasSession(userState);
                }
            } else {
                logger.error("toggleWalletSession is not empty, but has no params! {}, user {}", toggleWalletSession, userState);
            }
        }
    }
}
