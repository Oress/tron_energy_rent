package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.exception.UserAlreadyHasGroupBalanceException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.UsersShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ManageGroupNewGroupHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private BalanceService balanceService;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL:
                handleAddGroupPromptLabel(userState, update);
                break;
            case ADMIN_MANAGE_GROUPS_ADD_PROMPT_USERS:
                handleAddGroupPromptUsers(userState, update);
                break;
        }
    }

    private void handleAddGroupPromptLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            // TODO: validate group name
            String newGroupLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            AddGroupState addGroupState = telegramState.getOrCreateAddGroupState(telegramId);
            telegramState.updateAddGroupState(telegramId, addGroupState.withLabel(newGroupLabel));

            Message promptMsg = telegramMessages.manageGroupView().updMenuToManageGroupsAddPromptUsers(userState);
            telegramState.updateUserState(telegramId, userState
                    .withMessagesToDelete(List.of(promptMsg.getMessageId()))
                    .withState(States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_USERS));
        }
    }

    private void handleAddGroupPromptUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasUserShared()) {
            telegramMessages.deleteMessage(message);

            // TODO: validate group name.
            UsersShared usersShared = message.getUsersShared();
            List<UserShared> users = usersShared.getUsers();

            AddGroupState addGroupState = telegramState.getOrCreateAddGroupState(userState.getTelegramId());
            List<Long> userIds = users.stream().map(user -> user.getUserId()).toList();
            try {
                Balance groupBalance = balanceService.createGroupBalance(addGroupState.getLabel(), userIds);
            } catch (UserNotRegisteredException e) {
                // TODO: send message to user that some users are not registered, and specify
                // which ones
            } catch (UserAlreadyHasGroupBalanceException e) {
                // TODO: send message to user that some users are already in the group, and
                // specify which ones
            }

            telegramMessages.manageGroupView().updMenuToManageGroupsAddSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ADD_SUCCESS));
        }
    }
}