package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AdminMenuHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AdminViews adminViews;
    private final RestClient restClient;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            logger.info("Received callback query: {}", data);

            if (InlineMenuCallbacks.MANAGE_GROUPS.equals(data)) {
                telegramMessages.manageGroupView().updMenuToManageGroupsMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS));
            } else if (InlineMenuCallbacks.MANAGE_USERS.equals(data)) {

            } else if (InlineMenuCallbacks.MANAGE_ITRX_BALANCE.equals(data)) {
                ApiUsageResponse apiStats = restClient.getApiStats();
                adminViews.itrxBalance(callbackQuery, apiStats);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_VIEW_ITRX_BALANCE));
            }
        }
    }
}