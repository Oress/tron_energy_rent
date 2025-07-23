package org.ipan.nrgyrent.telegram.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.exception.WalletAlreadyHasActiveSessionException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoTopupSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.InactiveAddressException;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.CreateDelegatePolicyResponse;
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
    private final RestClient itrxRestClient;
    private final UserService userService;
    private final UserWalletService userWalletService;

    @MatchStates({
            @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.AUTOTOPUP),
            @MatchState(state = States.AUTODELEGATE_START_ERROR, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void handleAutoDelegationMenu(UserState userState, Update update) {
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
                AutoDelegationSession session = energyService.deactivateSessionManually(toggleWalletSession.getSessionId());
                autoDelegationViews.autoDelegateSessionStoppedManually(userState, session);

                List<WalletWithAutoTopupSession> walletsWithSessions = autoDelegationSessionRepo.findActiveSessionsWithWalletInfo(userState.getTelegramId());
                Message message = autoDelegationViews.autoDelegationMenuMsg(userState, walletsWithSessions);
                telegramState.updateUserState(userState.getTelegramId(), userState.withMenuMessageId(message.getMessageId()));
            } else if (toggleWalletSession.getAddress() != null) {
                // If wallet is not null that means that it is a request to start a session.
                AutoDelegationSession newSession = null;
                try {
                    newSession = energyService.startAutoTopupSession(userState, toggleWalletSession.getAddress());
                    CreateDelegatePolicyResponse createDelegatePolicyResponse = itrxRestClient.createDelegatePolicy(0, toggleWalletSession.getAddress());

                    if (createDelegatePolicyResponse.getErrno() != 0) {
                        logger.error("Something went wrong when creating a auto delegation session, response {}", createDelegatePolicyResponse);
                        throw new RuntimeException("Something went wrong when creating a auto delegation session");
                    }

                    autoDelegationViews.autoDelegateSessionCreated(userState, newSession);
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

//                    energyService.tryMakeFirstAutoTopupAsync(newSession.getId());
                } catch (WalletAlreadyHasActiveSessionException e) {
                    autoDelegationViews.walletIsAlreadyHasSession(userState);
                } catch (NotEnoughBalanceException e) {
                    autoDelegationViews.cannotStartAutoDelegateSessionLowBalance(userState);
                    telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTODELEGATE_START_ERROR));
                } catch (InactiveAddressException e) {
                    if (newSession != null) {
                        energyService.deactivateSessionInactiveWallet(newSession.getId());
                    }
                    autoDelegationViews.inactiveWallet(userState);
                } catch (Exception e) {
                    logger.error("Exception during autodelegation ", e);
                    if (newSession != null) {
                        energyService.deactivateSessionInitProblem(newSession.getId());
                    }
                    autoDelegationViews.unableToStartInitProblem(userState);
                }
            } else {
                logger.error("toggleWalletSession is not empty, but has no params! {}, user {}", toggleWalletSession, userState);
            }
        }
    }
}
