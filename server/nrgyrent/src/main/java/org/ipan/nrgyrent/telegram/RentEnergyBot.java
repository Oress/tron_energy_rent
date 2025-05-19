package org.ipan.nrgyrent.telegram;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.telegram.handlers.AdminMenuHandler;
import org.ipan.nrgyrent.telegram.handlers.MainMenuHandler;
import org.ipan.nrgyrent.telegram.handlers.ManageGroupActionsHandler;
import org.ipan.nrgyrent.telegram.handlers.ManageGroupNewGroupHandler;
import org.ipan.nrgyrent.telegram.handlers.ManageGroupSearchHandler;
import org.ipan.nrgyrent.telegram.handlers.ManageGroupsHandler;
import org.ipan.nrgyrent.telegram.handlers.TransactionsHandler;
import org.ipan.nrgyrent.telegram.handlers.UserWalletsHandler;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class RentEnergyBot implements LongPollingSingleThreadUpdateConsumer {
    public static final String START = "/start";
    public static final int WAIT_FOR_CALLBACK = 10;
    public static final int ITRX_OK_CODE = 0;

    private TelegramState telegramState;
    private TelegramMessages telegramMessages;
    private UserService userService;

    private final MainMenuHandler mainMenuHandler;
    private final UserWalletsHandler userWalletsHandler;
    private final TransactionsHandler transactionsHandler;
    private final AdminMenuHandler adminMenuHandler;
    private final ManageGroupSearchHandler manageGroupSearchHandler;
    private final ManageGroupNewGroupHandler manageGroupNewGroupHandler;
    private final ManageGroupsHandler manageGroupsHandler;
    private final ManageGroupActionsHandler manageGroupActionsHandler;

    @Override
    public void consume(Update update) {
        User from = getFrom(update);
        logger.info("Received update from user: {}", from);
        if (from.getIsBot()) {
            logger.info("Ignoring update from bot: {}", from);
            // Ignore bots
            return;
        }

        Long userId = from.getId();
        UserState userState = telegramState.getOrCreateUserState(userId);

        List<Integer> messagesToDelete = userState.getMessagesToDelete();
        if (messagesToDelete != null && !messagesToDelete.isEmpty()) {
            telegramMessages.deleteMessages(userState.getChatId(), messagesToDelete);
            userState = telegramState.updateUserState(userId, userState.withMessagesToDelete(null));
        }

        logger.info("User state: {}", userState);

        if (handleStartState(userState, update)) {
            return;
        }

        tryRemoveNotification(userState, update);

        switch (userState.getState()) {
            case MAIN_MENU:
                mainMenuHandler.handleUpdate(userState, update);
                break;
            case WALLETS:
            case NEW_WALLET_PROMPT_ADDRESS:
            case NEW_WALLET_PROMPT_LABEL:
                userWalletsHandler.handleUpdate(userState, update);
                break;
            case TRANSACTION_65k:
            case TRANSACTION_131k:
                transactionsHandler.handleUpdate(userState, update);
                break;

            // admin
            case ADMIN_MENU:
                adminMenuHandler.handleUpdate(userState, update);
                break;
            case ADMIN_MANAGE_GROUPS:
                manageGroupsHandler.handleUpdate(userState, update);
            case ADMIN_MANAGE_GROUPS_ACTION_PREVIEW:
            case ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM:
            case ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL:
            case ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS:
            case ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS:
            case ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE:
                manageGroupActionsHandler.handleUpdate(userState, update);
                break;
            case ADMIN_MANAGE_GROUPS_SEARCH:
                manageGroupSearchHandler.handleUpdate(userState, update);
                break;
            case ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL:
            case ADMIN_MANAGE_GROUPS_ADD_PROMPT_USERS:
            case ADMIN_MANAGE_GROUPS_REMOVE_PROMPT_USERS:
                manageGroupNewGroupHandler.handleUpdate(userState, update);
                break;
        }

        Message message = update.getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (message != null && message.hasText()) {
            telegramMessages.deleteMessage(message);
        } else if (callbackQuery != null) {
            String data = callbackQuery.getData();

            EditMessageText.builder().replyMarkup(InlineKeyboardMarkup.builder().build());
            if (InlineMenuCallbacks.TO_MAIN_MENU.equals(data)) {
                switch (userState.getRole()) {
                    case ADMIN:
                        telegramMessages.updateMsgToAdminMainMenu(callbackQuery);
                        break;
                    default:
                        telegramMessages.updateMsgToMainMenu(callbackQuery);
                        break;
                }
                telegramState.updateUserState(userId, userState.withState(States.MAIN_MENU));
            }
        }
    }

    private void tryRemoveNotification(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null && InlineMenuCallbacks.NTFN_OK.equals(callbackQuery.getData())) {
            telegramMessages.deleteMessage(userState, callbackQuery);
        }
    }

    private boolean handleStartState(UserState userState, Update update) {
        Message message = update.getMessage();

        if (message != null && message.hasText()) {
            String text = message.getText();

            if (START.equals(text)) {
                // may be null if user is not registered
                AppUser user = userService.getById(userState.getTelegramId());
                UserRole role = user != null ? user.getRole() : UserRole.USER;

                // TODO: remove keyboard from it exists
                Message newMenuMsg = switch (role) {
                    case ADMIN -> telegramMessages.sendAdminMainMenu(update.getMessage().getChatId());
                    case USER -> telegramMessages.sendMainMenu(update.getMessage().getChatId());
                    default -> telegramMessages.sendMainMenu(update.getMessage().getChatId());
                };

                telegramState.updateUserState(userState.getTelegramId(), userState
                        .withState(States.MAIN_MENU)
                        .withChatId(newMenuMsg.getChatId())
                        .withRole(role)
                        .withMenuMessageId(newMenuMsg.getMessageId()));
                if (user == null) {
                    userService.createUser(
                            CreateUserCommand.builder()
                                    .telegramId(userState.getTelegramId())
                                    .firstName(message.getFrom().getFirstName())
                                    .username(message.getFrom().getUserName())
                                    .build());
                } else {
                    userService.updateUser(
                            CreateUserCommand.builder()
                                    .telegramId(userState.getTelegramId())
                                    .firstName(message.getFrom().getFirstName())
                                    .username(message.getFrom().getUserName())
                                    .build());
                }
                telegramMessages.deleteMessage(message);

                // remove old menu message if exists
                if (userState.getMenuMessageId() != null) {
                    telegramMessages.deleteMessage(userState.getChatId(), userState.getMenuMessageId());
                }

                return true;
            }
        }
        return false;
    }

    private User getFrom(Update update) {
        Message message = update.getMessage();
        if (message != null && message.getFrom() != null) {
            return message.getFrom();
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null && callbackQuery.getFrom() != null) {
            return callbackQuery.getFrom();
        }

        // TODO: make custom exception for this.
        throw new IllegalStateException("Cannot determine user from update: " + update);
    }

}
