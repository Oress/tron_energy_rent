package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class UsersActionHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final ManageUserActionsView manageUserActionsView;
    private final ManageUsersSearchHandler manageUsersSearchHandler;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case ADMIN_MANAGE_USERS_ACTION_PREVIEW:
                handleGroupPreivew(userState, update);
                break;

            // case ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE:
            // handleAdjustedBalance(userState, update);
            // break;

            // case ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS:
            // handleRemoveUsers(userState, update);
            // break;

            case ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM:
                handleUserDeleteConfirm(userState, update);
                break;

            default:
                break;
        }
    }

    private void handleGroupPreivew(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            if (InlineMenuCallbacks.MANAGE_USER_ACTION_DEACTIVATE.equals(data)) {
                handleDeactivateUser(userState, callbackQuery);
            } else if (InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY.equals(data)) {
                manageUserActionsView.promptNewUserBalance(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_USER_ACTION_PROMPT_NEW_BALANCE));
            }
        }
    }

    private void handleDeactivateUser(UserState userState, CallbackQuery callbackQuery) {
        manageUserActionsView.confirmDeactivateUserMsg(callbackQuery);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM));
    }

    private void handleUserDeleteConfirm(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            UserEdit openUser = telegramState.getOrCreateUserEdit(userState.getTelegramId());
            if (data.equals(InlineMenuCallbacks.CONFIRM_YES)) {
                // TODO: delete group balance, remove group balance from users, watch out for
                // potential actions with deleted group
                // TODO: handle balance not found exception

                userService.deactivateUser(openUser.getSelectedUserId());
                manageUserActionsView.userDeleted(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DELETE_SUCCESS));
            } else if (data.equals(InlineMenuCallbacks.CONFIRM_NO)) {
                manageUsersSearchHandler.openUser(userState, callbackQuery, openUser.getSelectedUserId());
            } else {
                logger.error("Unknown callback data when confirming group deletion: {}", data);
            }
        }
    }
}
