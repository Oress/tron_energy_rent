package org.ipan.nrgyrent.telegram.handlers;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.AutoAmlService;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSession;
import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSessionDeactivationReason;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoAmlSession;
import org.ipan.nrgyrent.domain.model.repository.AutoAmlSessionRepo;
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
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.AutoAmlViews;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@TransitionHandler
@Slf4j
public class AutoAmlHandler {
    private final AutoAmlViews autoAmlViews;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AutoAmlSessionRepo autoAmlSessionRepo;
    private final AutoAmlService autoAmlService;
    private final UserService userService;
    private final UserWalletService userWalletService;
    private final ParseUtils parseUtils;

    private final ConcurrentHashMap<Long, String> pendingAddresses = new ConcurrentHashMap<>();

    public AutoAmlHandler(AutoAmlViews autoAmlViews,
                          TelegramMessages telegramMessages,
                          TelegramState telegramState,
                          AutoAmlSessionRepo autoAmlSessionRepo,
                          AutoAmlService autoAmlService,
                          UserService userService,
                          ParseUtils parseUtils,
                          UserWalletService userWalletService) {
        this.autoAmlViews = autoAmlViews;
        this.telegramMessages = telegramMessages;
        this.telegramState = telegramState;
        this.autoAmlSessionRepo = autoAmlSessionRepo;
        this.autoAmlService = autoAmlService;
        this.userService = userService;
        this.userWalletService = userWalletService;
        this.parseUtils = parseUtils;
    }

    @MatchStates({
            @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AUTO_AML),
            @MatchState(state = States.AUTO_AML_THRESHOLD_PROMPT, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AUTO_AML_START_ERROR, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void showAutoAmlMenu(UserState userState, Update update) {
        pendingAddresses.remove(userState.getTelegramId());
        List<WalletWithAutoAmlSession> walletsWithSessions = autoAmlSessionRepo.findActiveSessionsWithWalletInfo(userState.getTelegramId());
        autoAmlViews.showAutoAmlMenu(userState, walletsWithSessions);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTO_AML_MENU));
    }

    @MatchState(state = States.AUTO_AML_MENU, updateTypes = UpdateType.CALLBACK_QUERY)
    public void toggleAutoAmlSession(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        InlineMenuCallbacks.ToggleWalletSessionPayload payload = InlineMenuCallbacks.getToggleAutoAmlSession(data);

        if (payload == null) {
            return;
        }

        if (payload.getSessionId() != null) {
            // Deactivate existing session
            AutoAmlSession session = autoAmlService.deactivateSession(payload.getSessionId(), AutoAmlSessionDeactivationReason.MANUAL);
            autoAmlViews.showSessionStopped(userState, session);

            List<WalletWithAutoAmlSession> walletsWithSessions = autoAmlSessionRepo.findActiveSessionsWithWalletInfo(userState.getTelegramId());
            Message message = autoAmlViews.showAutoAmlMenuMsg(userState, walletsWithSessions);
            telegramState.updateUserState(userState.getTelegramId(), userState.withMenuMessageId(message.getMessageId()));
        } else if (payload.getAddress() != null) {
            // Start new session — prompt for threshold
            pendingAddresses.put(userState.getTelegramId(), payload.getAddress());
            autoAmlViews.showThresholdPrompt(userState);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTO_AML_THRESHOLD_PROMPT));
        } else {
            logger.error("toggleAutoAmlSession: payload has no params! {}, user {}", payload, userState);
        }
    }

    @MatchState(state = States.AUTO_AML_THRESHOLD_PROMPT, updateTypes = UpdateType.MESSAGE)
    public void handleThresholdInput(UserState userState, Update update) {
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        String input = message.getText().trim();
        Long thresholdSun;
        try {
            thresholdSun = parseUtils.parseTrxStrToSunLong(input);
            if (thresholdSun < 1_000_000) {
                autoAmlViews.showInvalidThreshold(userState);
                return;
            }
        } catch (NumberFormatException e) {
            autoAmlViews.showInvalidThreshold(userState);
            return;
        }

        String address = pendingAddresses.remove(userState.getTelegramId());
        if (address == null) {
            logger.error("No pending address for user {} during threshold input", userState.getTelegramId());
            return;
        }

        try {
            AutoAmlSession session = autoAmlService.createSession(
                    userState.getTelegramId(), address, thresholdSun,
                    userState.getChatId(), userState.getMenuMessageId());

            autoAmlViews.showSessionCreated(userState, session);

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
        } catch (IllegalStateException e) {
            logger.warn("Cannot create auto-AML session: {}", e.getMessage());
            autoAmlViews.showInvalidThreshold(userState);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTO_AML_START_ERROR));
        } catch (Exception e) {
            logger.error("Error creating auto-AML session", e);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AUTO_AML_START_ERROR));
        }
    }
}
