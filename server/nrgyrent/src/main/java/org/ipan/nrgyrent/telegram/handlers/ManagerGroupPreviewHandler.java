package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
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
public class ManagerGroupPreviewHandler {
    private final ManageGroupActionsView manageGroupActionsView;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final BalanceService balanceService;

    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
    public void startAddUsers_promptUsers(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        manageGroupActionsView.updMenuPromptToAddUsersToGroup(callbackQuery);
        Message msg = manageGroupActionsView.promptToAddUsersToGroup(callbackQuery);
        // TODO: in case user input something else, the message will be deleted, handle
        // it
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.MANAGER_GROUPS_ACTION_ADD_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    
    @MatchState(state = States.MANAGER_GROUPS_ACTION_ADD_USERS, updateTypes = UpdateType.MESSAGE)
    public void handleUsers_end(UserState userState, Update update) {
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

    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
    public void startRemoveUsers(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        manageGroupActionsView.updMenuPromptToRemoveUsersFromGroup(callbackQuery);
        Message msg = manageGroupActionsView.promptToRemoveUsersToGroup(callbackQuery);
        // TODO: in case user input something else, the message will be deleted, handle
        // it
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.MANAGER_GROUPS_ACTION_REMOVE_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(state = States.MANAGER_GROUPS_ACTION_REMOVE_USERS, updateTypes = UpdateType.MESSAGE)
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


    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
    public void viewGroupUsers(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(openBalance.getSelectedBalanceId());
        manageGroupActionsView.reviewGroupUsers(update.getCallbackQuery(), users);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MANAGER_GROUP_VIEW_USERS));
    }

}