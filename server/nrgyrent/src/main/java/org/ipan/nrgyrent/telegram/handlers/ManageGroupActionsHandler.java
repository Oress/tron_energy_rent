package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
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
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ManageGroupActionsHandler implements AppUpdateHandler {
    private final ManageGroupActionsView manageGroupActionsView;
    private final TelegramState telegramState;
    private final BalanceRepo balanceRepo;
    private final BalanceService balanceService;
    private final ManageGroupSearchHandler manageGroupSearchHandler;

    @Override
    public void handleUpdate(UserState userState, Update update) {

        switch (userState.getState()) {
            case ADMIN_MANAGE_GROUPS_ACTION_PREVIEW:
                handleGroupPreivew(userState, update);
                break;

            case ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL:
                handleNewLabel(userState, update);
                break;

            case ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM:
                handleGroupDeleteConfirm(userState, update);
                break;

            default:
                break;
        }
    }

    private void handleNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            logger.info("Renaming group with new label: {}", message.getText());
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.info("New label is too short: {}", newLabel);
                // TODO: send warning message
                return;
            }

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.renameGroupBalance(openBalance.getSelectedBalanceId(), newLabel);

            manageGroupActionsView.groupRenamed(userState);
        }
    }

    private void handleGroupPreivew(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            if (InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DEACTIVATE.equals(data)) {
                handleDeactivateGroup(userState, callbackQuery);
            } else if (InlineMenuCallbacks.MANAGE_GROUPS_ACTION_RENAME.equals(data)) {
                manageGroupActionsView.promptNewGroupLabel(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL));
            } else if (InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS.equals(data)) {
                BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
                Balance balance = balanceRepo.findByIdWithUsers(openBalance.getSelectedBalanceId()).orElse(null);
                manageGroupActionsView.reviewGroupUsers(callbackQuery, balance.getUsers());
                
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_USERS_REVIEW));
            }
        }
    }

    private void handleDeactivateGroup(UserState userState, CallbackQuery callbackQuery) {
        manageGroupActionsView.confirmDeactivateGroupMsg(callbackQuery);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM));
    }

    private void handleGroupDeleteConfirm(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
            if (data.equals(InlineMenuCallbacks.CONFIRM_YES)) {
                // TODO: delete group balance, remove group balance from users, watch out for
                // potential actions with deleted group
                // TODO: handle balance not found exception
                balanceService.deactivateGroupBalance(openBalance.getSelectedBalanceId());
                manageGroupActionsView.groupDeleted(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DELETE_SUCCESS));
            } else if (data.equals(InlineMenuCallbacks.CONFIRM_NO)) {
                manageGroupSearchHandler.openGroupBalance(userState, callbackQuery, openBalance.getSelectedBalanceId());
            } else {
                logger.error("Unknown callback data when confirming group deletion: {}", data);
            }
        }
    }
}