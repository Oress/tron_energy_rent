package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageGroupActionsView;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UsersShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class ManageGroupActionsHandler {
    private final ManageGroupActionsView manageGroupActionsView;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final BalanceService balanceService;
    private final ManageGroupSearchHandler manageGroupSearchHandler;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_MANAGER)
    public void startChangeManager_promptManager(UserState userState, Update update) {
        manageGroupActionsView.updMenuPromptManager(userState);
        Message message = manageGroupActionsView.sendPromptManager(userState);

        telegramState.updateUserState(userState.getTelegramId(),
                userState.withMessagesToDelete(List.of(message.getMessageId()))
                        .withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_MANAGER));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_MANAGER, updateTypes = UpdateType.MESSAGE)
    public void handleManager_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasUserShared()) {
            telegramMessages.deleteMessage(message);
            logger.info("Changing manager to: {}", message.getText());
            UsersShared usersShared = message.getUsersShared();
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.changeManager(openBalance.getSelectedBalanceId(), usersShared.getUsers().get(0).getUserId());

            manageGroupActionsView.updMenuManagerChanged(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_MANAGER_CHANGED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADJUST_BALANCE_MANUALLY)
    public void startAdjustBalanceManually(UserState userState, Update update) {
        manageGroupActionsView.promptNewGroupBalance(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_RENAME)
    public void startRenaming(UserState userState, Update update) {
        manageGroupActionsView.promptNewGroupLabel(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
    public void startAddUsers(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        manageGroupActionsView.updMenuPromptToAddUsersToGroup(callbackQuery);
        Message msg = manageGroupActionsView.promptToAddUsersToGroup(callbackQuery);
        // TODO: in case user input something else, the message will be deleted, handle
        // it
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
    public void startRemoveUsers(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        manageGroupActionsView.updMenuPromptToRemoveUsersFromGroup(callbackQuery);
        Message msg = manageGroupActionsView.promptToRemoveUsersToGroup(callbackQuery);
        // TODO: in case user input something else, the message will be deleted, handle
        // it
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
    public void viewGroupUsers(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(openBalance.getSelectedBalanceId());
        manageGroupActionsView.reviewGroupUsers(update.getCallbackQuery(), users);

        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_USERS_REVIEW));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS, updateTypes = UpdateType.MESSAGE)
    private void handleRemoveUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasUserShared()) {
            telegramMessages.deleteMessage(message);
            logger.info("Removing users from group: {}", message.getText());
            UsersShared usersShared = message.getUsersShared();
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<Long> userIds = usersShared.getUsers().stream().map(user -> user.getUserId()).toList();
            // TODO: handle errors
            balanceService.removeUsersFromTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);

            manageGroupActionsView.groupUsersRemoved(userState);
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS, updateTypes = UpdateType.MESSAGE)
    public void handleAddNewUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasUserShared()) {
            telegramMessages.deleteMessage(message);
            logger.info("Adding new users to group: {}", message.getText());
            UsersShared usersShared = message.getUsersShared();
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<Long> userIds = usersShared.getUsers().stream().map(user -> user.getUserId()).toList();
            // TODO: handle errors
            balanceService.addUsersToTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);

            manageGroupActionsView.groupUsersAdded(userState);
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Renaming group with new label: {}", message.getText());
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                manageGroupActionsView.groupNameIsTooShort(userState);
                return;
            }

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.renameGroupBalance(openBalance.getSelectedBalanceId(), newLabel);

            manageGroupActionsView.groupRenamed(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_RENAMED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE, updateTypes = UpdateType.MESSAGE)
    public void handleAdjustedBalance(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Adjusting group balance: {}", message.getText());
            String newBalance = message.getText();
            // TODO: catch NumberFormatException
            BigDecimal adjustedBalanceInTrx = new BigDecimal(newBalance);
            BigDecimal adjustedBalanceInSun = adjustedBalanceInTrx.multiply(AppConstants.trxToSunRate);
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.adjustBalance(openBalance.getSelectedBalanceId(), adjustedBalanceInSun.longValue(),
                    telegramId);

            manageGroupActionsView.groupBalanceAdjusted(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_BALANCE_ADJUSTED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DEACTIVATE)
    public void handleDeactivateGroup(UserState userState, Update update) {
        manageGroupActionsView.confirmDeactivateGroupMsg(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_YES)
    public void confirmGroupDeactivate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        balanceService.deactivateGroupBalance(openBalance.getSelectedBalanceId());
        manageGroupActionsView.groupDeleted(callbackQuery);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_SUCCESS));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO)
    public void declineGroupDeactivate(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        manageGroupSearchHandler.openGroupBalance(userState, openBalance.getSelectedBalanceId());
    }
}