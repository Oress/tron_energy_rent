package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.exception.UserAlreadyHasGroupBalanceException;
import org.ipan.nrgyrent.domain.exception.UserIsManagerException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.exception.UsersMustBelongToTheSameGroupException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
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
    private final UserService userService;

    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
    public void startAddUsers_promptUsers(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());

        if (!user.isGroupManager()) {
            logger.error("User not a manager, user {}", userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        if (!user.getGroupBalance().getId().equals(openBalance.getSelectedBalanceId())) {
            logger.error("User does not manage this group opened balance: {},  user: {}", openBalance, userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        manageGroupActionsView.updMenuPromptToAddUsersToGroup(userState);
        Message msg = manageGroupActionsView.promptToAddUsersToGroup(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.MANAGER_GROUPS_ACTION_ADD_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    
    @MatchState(state = States.MANAGER_GROUPS_ACTION_ADD_USERS, updateTypes = UpdateType.MESSAGE)
    public void handleUsers_end(UserState userState, Update update) {
        Message message = update.getMessage();
        UsersShared usersShared = message.getUsersShared();
        if (message != null && usersShared.getUsers() != null && !usersShared.getUsers().isEmpty()) {
            telegramMessages.deleteMessage(message);
            logger.info("Adding new users to group: {}", message.getText());
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<TgUserId> userIds = usersShared.getUsers().stream().map(user -> new TgUserId(user.getUserId(), user.getUsername(), user.getFirstName())).toList();
            try {
                balanceService.addUsersToTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);
            } catch (UserNotRegisteredException e) {
                logger.error("Error adding users to group: {}", e.getMessage());
                manageGroupActionsView.someUsersAreNotRegistered(userState, e.getUserIds());
                return;
            } catch (UserAlreadyHasGroupBalanceException e) {
                logger.error("Error adding users to group: {}", e.getMessage());
                manageGroupActionsView.userBelongsToAnotherGroup(userState);
                return;
            } catch (Exception e) {
                logger.error("Error adding users to group: {}", e.getMessage());
                manageGroupActionsView.somethingWentWrong(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());
            userState = telegramState.updateUserState(telegramId, userState.withState(States.MANAGER_GROUPS_ACTION_ADD_USERS_SUCCESS).withMessagesToDelete(null));
            manageGroupActionsView.groupUsersAdded(userState);
        }
    }

    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
    public void startRemoveUsers(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());

        if (!user.isGroupManager()) {
            logger.error("User not a manager, user {}", userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        if (!user.getGroupBalance().getId().equals(openBalance.getSelectedBalanceId())) {
            logger.error("User does not manage this group opened balance: {},  user: {}", openBalance, userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        manageGroupActionsView.updMenuPromptToRemoveUsersFromGroup(userState);
        Message msg = manageGroupActionsView.promptToRemoveUsersToGroup(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.MANAGER_GROUPS_ACTION_REMOVE_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(state = States.MANAGER_GROUPS_ACTION_REMOVE_USERS, updateTypes = UpdateType.MESSAGE)
    public void handleRemoveUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        UsersShared usersShared = message.getUsersShared();
        if (message != null && usersShared.getUsers() != null && !usersShared.getUsers().isEmpty()) {
            telegramMessages.deleteMessage(message);
            logger.info("Removing users from group: {}", message.getText());
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<TgUserId> userIds = usersShared.getUsers().stream().map(user -> new TgUserId(user.getUserId(), user.getUsername(), user.getFirstName())).toList();
            try {
                if (userIds.stream().anyMatch(u -> u.getId().equals(telegramId))) {
                    logger.error("manager is trying to delete themself. balanceId: {} user: {}", openBalance.getSelectedBalanceId(),userState);
                    throw new UserIsManagerException("manager is trying to delete themself.");
                }

                balanceService.removeUsersFromTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);
            } catch (UserNotRegisteredException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.someUsersAreNotRegistered(userState, e.getUserIds());
                return;
            } catch (UserIsManagerException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.cannotRemoveManager(userState);
                return;
            }  catch (UsersMustBelongToTheSameGroupException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.cannotRemoveManager(userState);
                return;
            } catch (Exception e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.somethingWentWrong(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());
            userState = telegramState.updateUserState(telegramId, userState.withState(States.MANAGER_GROUPS_ACTION_REMOVE_USERS_SUCCESS).withMessagesToDelete(null));

            manageGroupActionsView.groupUsersRemoved(userState);
        }
    }


    @MatchState(state = States.MANAGER_GROUP_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
    public void viewGroupUsers(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());

        if (!user.isGroupManager()) {
            logger.error("User not a manager, user {}", userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        if (!user.getGroupBalance().getId().equals(openBalance.getSelectedBalanceId())) {
            logger.error("User does not manage this group opened balance: {},  user: {}", openBalance, userState);
            manageGroupActionsView.userNotManager(userState);
            return;
        }

        Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(openBalance.getSelectedBalanceId());
        manageGroupActionsView.reviewGroupUsers(userState, users);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MANAGER_GROUP_VIEW_USERS));
    }

}