package org.ipan.nrgyrent.telegram.handlers;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class ManageUsersSearchHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final ManageUserActionsView manageUserActionsView;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            logger.info("Searching for users with username: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            // TODO: validate query string ??
            if (queryStr.length() < 3) {
                logger.info("Query string is too short: {}", queryStr);
                // telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(null,
                // message);
                return;
            }

            Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId(queryStr,
                    PageRequest.of(0, 10));
            manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        // Handle search reset + pagination + group selection
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.MANAGE_USERS_SEARCH_RESET.equals(data)) {
                Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId("",
                        PageRequest.of(0, 10));
                manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
            } else if (data.startsWith(ManageUserActionsView.OPEN_BALANCE)) {
                String telegramIdStr = data.split(ManageUserActionsView.OPEN_BALANCE)[1];
                Long telegramId = Long.parseLong(telegramIdStr);
                openUser(userState, callbackQuery, telegramId);
            }
        }
    }

    public void openUser(UserState userState, CallbackQuery callbackQuery, Long telegramId) {
        Optional<AppUser> appUser = appUserRepo.findById(telegramId);
        if (appUser.isPresent()) {
            AppUser user = appUser.get();
            // TODO: make the message to show more details: name, balance, address, manager.
            manageUserActionsView.updMenuToManageUserActionsMenu(callbackQuery, user);
            UserEdit userEdit = telegramState.getOrCreateUserEdit(userState.getTelegramId());
            telegramState.updateUserEdit(userState.getTelegramId(), userEdit.withSelectedUserId(telegramId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USERS_ACTION_PREVIEW));
        } else {
            logger.error("Group balance not found for ID: {}", telegramId);
        }
    }
}
