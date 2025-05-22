package org.ipan.nrgyrent.telegram.handlers;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class ManageUsersSearchHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final ManageUserActionsView manageUserActionsView;

    @MatchStates({
        @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_USERS),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USERS, callbackData = InlineMenuCallbacks.MANAGE_USERS_SEARCH_RESET),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void showManageUsersMenu(UserState userState, Update update) {
        Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId("",
                PageRequest.of(0, 10));
        manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USERS));
    }

    @MatchStates({
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USERS, updateTypes = UpdateType.CALLBACK_QUERY),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM, updateTypes = UpdateType.CALLBACK_QUERY),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USER_ACTION_PROMPT_NEW_BALANCE, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openUserByCallback(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        UserEdit userEdit = telegramState.getOrCreateUserEdit(userState.getTelegramId());

        if (data.startsWith(ManageUserActionsView.OPEN_BALANCE)) {
            String telegramIdStr = data.split(ManageUserActionsView.OPEN_BALANCE)[1];
            Long telegramId = Long.parseLong(telegramIdStr);
            openUser(userState, telegramId);
        } else if (userEdit.getSelectedUserId() != null && userState.getState() != States.ADMIN_MANAGE_USERS) {
            Long userId = userEdit.getSelectedUserId();
            openUser(userState, userId);
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USERS, updateTypes = UpdateType.MESSAGE)
    public void searchUsersByUsername(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Searching for users with username: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            if (queryStr.length() < 3) {
                logger.info("Query string is too short: {}", queryStr);
                // telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(null,
                // message);
                return;
            }

            Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId(
                    queryStr,
                    PageRequest.of(0, 10));
            manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
        }
    }

    public void openUser(UserState userState, Long telegramId) {
        Optional<AppUser> appUser = appUserRepo.findById(telegramId);
        if (appUser.isPresent()) {
            AppUser user = appUser.get();
            manageUserActionsView.updMenuToManageUserActionsMenu(userState, user);
            UserEdit userEdit = telegramState.getOrCreateUserEdit(userState.getTelegramId());
            telegramState.updateUserEdit(userState.getTelegramId(), userEdit.withSelectedUserId(telegramId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USERS_ACTION_PREVIEW));
        } else {
            logger.error("Group balance not found for ID: {}", telegramId);
        }
    }
}
