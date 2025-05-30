package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.exception.UserIsManagerException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageGroupActionsView;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.UsersShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class ManageGroupNewGroupHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final BalanceService balanceService;

    private final ManageGroupNewGroupView manageGroupNewGroupView;
    private final ManageGroupActionsView manageGroupActionsView;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ADD)
    public void start_promptLabel(UserState userState, Update update) {
        telegramMessages.manageGroupView().updMenuToManageGroupsAddPromptLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleLabel_promptManager(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String newGroupLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newGroupLabel.length() < 3) {
                logger.warn("New label is too short: {}", newGroupLabel);
                manageGroupActionsView.groupNameIsTooShort(userState);
                return;
            }

            AddGroupState addGroupState = telegramState.getOrCreateAddGroupState(telegramId);
            telegramState.updateAddGroupState(telegramId, addGroupState.withLabel(newGroupLabel));

            manageGroupNewGroupView.updMenuPromptManager(userState);
            Message promptMsg = manageGroupNewGroupView.sendAddPromptManager(userState);
            telegramState.updateUserState(telegramId, userState
                    .withMessagesToDelete(List.of(promptMsg.getMessageId()))
                    .withState(States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_MANAGER));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_MANAGER, updateTypes = UpdateType.MESSAGE)
    public void handleManager_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasUserShared()) {
            telegramMessages.deleteMessage(message);

            UsersShared usersShared = message.getUsersShared();
            UserShared manager = usersShared.getUsers().getFirst();

            AddGroupState addGroupState = telegramState.getOrCreateAddGroupState(userState.getTelegramId());
            // List<Long> userIds = users.stream().map(user -> user.getUserId()).toList();
            Long managerId = manager.getUserId();
            try {
                balanceService.createGroupBalance(addGroupState.getLabel(), managerId, AppConstants.DEFAULT_TARIFF_ID);
            } catch (UserNotRegisteredException e) {
                manageGroupNewGroupView.someUsersAreNotRegistered(userState);
                return;
            } catch (UserIsManagerException e) {
                manageGroupNewGroupView.userIsManagerInAnotherGroup(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());

            telegramMessages.manageGroupView().updMenuToManageGroupsAddSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withMessagesToDelete(null).withState(States.ADMIN_MANAGE_GROUPS_ADD_SUCCESS));
        }
    }
}