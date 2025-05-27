package org.ipan.nrgyrent.telegram;

import java.lang.reflect.Method;
import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.StateHandlerRegistry;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionMatcher;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;

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

    private final StateHandlerRegistry stateHandlerRegistry;

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

        CallbackQuery callbackQuery = update.getCallbackQuery();
        List<Integer> messagesToDelete = userState.getMessagesToDelete();
        if (callbackQuery != null && messagesToDelete != null && !messagesToDelete.isEmpty() && (InlineMenuCallbacks.GO_BACK.equals(callbackQuery.getData()) || InlineMenuCallbacks.TO_MAIN_MENU.equals(callbackQuery.getData()))) {
            telegramMessages.deleteMessages(userState.getChatId(), messagesToDelete);
            userState = telegramState.updateUserState(userId, userState.withMessagesToDelete(null));
        }

        logger.info("User state: {}", userState);

        AppUser user = userService.getById(userId);
        if (user != null && Boolean.TRUE.equals(user.getDisabled())) {
            return;
        } else {
            if (user != null) {
                if (user.getGroupBalance() != null && user.getTelegramId() == user.getGroupBalance().getManager().getTelegramId()) {
                    userState = userState.withManagingGroupId(user.getGroupBalance().getId());
                } else {
                    userState = userState.withManagingGroupId(null);
                }
            }
        }

        if (handleStartState(user, userState, update)) {
            return;
        }

        if (tryRemoveNotification(userState, update)) {
            return;
        }

        int updateType = UpdateType.NONE;
        if (update.hasCallbackQuery()) {
            updateType |= UpdateType.CALLBACK_QUERY;
        } else if (update.hasMessage()) {
            updateType |= UpdateType.MESSAGE;
        }

        List<TransitionMatcher> handlers = stateHandlerRegistry.getHandlers(userState.getState(), updateType);

        for (TransitionMatcher handler : handlers) {
            if (!handler.matches(userState, update)) {
                continue;
            }

            Object stateHandler = handler.getBean();
            Method method = handler.getMethod();

            try {
                method.invoke(stateHandler, userState, update);
            } catch (Exception e) {
                logger.error("Error invoking state handler method", e);
            }
        }

        Message message = update.getMessage();

        if (message != null) {
            telegramMessages.deleteMessage(message);
        } else if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.TO_MAIN_MENU.equals(data)) {
                switch (userState.getRole()) {
                    case ADMIN:
                        telegramMessages.updateMsgToAdminMainMenu(userState, callbackQuery, user);
                        break;
                    default:
                        telegramMessages.updateMsgToMainMenu(userState, user);
                        break;
                }
                telegramState.updateUserState(userId, userState.withState(States.MAIN_MENU));
            }
        }
    }

    private boolean tryRemoveNotification(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null && InlineMenuCallbacks.NTFN_OK.equals(callbackQuery.getData())) {
            telegramMessages.deleteMessage(userState, callbackQuery);
            return true;
        }
        return false;
    }

    private boolean handleStartState(AppUser user, UserState userState, Update update) {
        Message message = update.getMessage();

        if (message != null && message.hasText()) {
            String text = message.getText();

            if (START.equals(text)) {
                if (user == null) {
                    user = userService.createUser(
                            CreateUserCommand.builder()
                                    .telegramId(userState.getTelegramId())
                                    .firstName(message.getFrom().getFirstName())
                                    .username(message.getFrom().getUserName())
                                    .build());
                }
                telegramMessages.deleteMessage(message);
                UserRole role = user != null ? user.getRole() : UserRole.USER;

                Message newMenuMsg = switch (role) {
                    case ADMIN -> telegramMessages.sendAdminMainMenu(userState, update.getMessage().getChatId(), user);
                    default -> telegramMessages.sendMainMenu(userState, update.getMessage().getChatId(), user);
                };

                telegramState.updateUserState(userState.getTelegramId(), userState
                        .withState(States.MAIN_MENU)
                        .withChatId(newMenuMsg.getChatId())
                        .withRole(role)
                        .withMenuMessageId(newMenuMsg.getMessageId()));

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
