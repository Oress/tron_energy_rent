package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AdminMenuHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.MANAGE_GROUPS.equals(data)) {
                telegramMessages.manageGroupView().updMenuToManageGroupsMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS));
            } else if (InlineMenuCallbacks.MANAGE_USERS.equals(data)) {

            }
        }
    }
}