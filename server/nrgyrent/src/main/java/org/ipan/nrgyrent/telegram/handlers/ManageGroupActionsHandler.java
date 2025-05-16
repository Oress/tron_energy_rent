package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.ManageGroupActionsView;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ManageGroupActionsHandler implements AppUpdateHandler {
    private final ManageGroupActionsView manageGroupActionsView;
    private final TelegramState telegramState;
    private final BalanceRepo balanceRepo;
    private final ManageGroupSearchHandler manageGroupSearchHandler;

    @Override
    public void handleUpdate(UserState userState, Update update) {

        switch (userState.getState()) {
            case ADMIN_MANAGE_GROUPS_ACTION_PREVIEW:
                handleGroupPreivew(userState, update);
                break;

            case ADMIN_MANAGE_GROUPS_ACTION_DELETE_CONFIRM:
                handleGroupDeleteConfirm(userState, update);
                break;

            default:
                break;
        }
    }

    private void handleGroupPreivew(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            if (data.equals(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DELETE)) {
                handleDeleteGroup(userState, callbackQuery);
            }
        }
    }

    private void handleDeleteGroup(UserState userState, CallbackQuery callbackQuery) {
        manageGroupActionsView.confirmDeleteGroupMsg(callbackQuery);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DELETE_CONFIRM));
    }

    private void handleGroupDeleteConfirm(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
            if (data.equals(InlineMenuCallbacks.CONFIRM_YES)) {
                // TODO: delete group balance, remove group balance from users, watch out for potential actions with deleted group
                Balance balance = balanceRepo.findById(openBalance.getSelectedBalanceId()).orElse(null);
                if (balance != null) {
                    balanceRepo.delete(balance);
                    manageGroupActionsView.groupDeleted(callbackQuery);
                    telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DELETE_SUCCESS));
                } else {
                    logger.error("Balance not found for deletion: {}", openBalance.getSelectedBalanceId());
                }
            } else if (data.equals(InlineMenuCallbacks.CONFIRM_NO)) {
                manageGroupSearchHandler.openGroupBalance(userState, callbackQuery, openBalance.getSelectedBalanceId());
            } else {
                logger.error("Unknown callback data when confirming group deletion: {}", data);
            }
        }
    }
}