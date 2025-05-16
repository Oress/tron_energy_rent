package org.ipan.nrgyrent.telegram.handlers;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.ManageGroupSearchView;
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
public class ManageGroupSearchHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private BalanceRepo balanceRepo;

    @Override
    public void handleUpdate(UserState userState, Update update) {
             Message message = update.getMessage();
        if (message != null && message.hasText()) {
            logger.info("Searching for groups with label: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            // TODO: validate query string ??
            if (queryStr.length() < 3) {
                logger.info("Query string is too short: {}", queryStr);
                // telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(null,
                // message);
                return;
            }

            Page<Balance> firstPage = balanceRepo.findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType.GROUP,
                    queryStr, PageRequest.of(0, 10));
            telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(firstPage, userState);
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        // Handle search reset + pagination + group selection
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.MANAGE_GROUPS_SEARCH_RESET.equals(data)) {
                Page<Balance> firstPage = balanceRepo.findAllByTypeOrderById(BalanceType.GROUP, PageRequest.of(0, 10));
                telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(firstPage, userState);
            } else if (data.startsWith(ManageGroupSearchView.OPEN_BALANCE)) {
                String balanceIdStr = data.split(ManageGroupSearchView.OPEN_BALANCE)[1];
                Long balanceId = Long.parseLong(balanceIdStr);
                openGroupBalance(userState, callbackQuery, balanceId);
            }
        }
    }

    public void openGroupBalance(UserState userState, CallbackQuery callbackQuery, Long balanceId) {
        Optional<Balance> groupBalance = balanceRepo.findById(balanceId);
        if (groupBalance.isPresent()) {
            Balance balance = groupBalance.get();
            // TODO: make the message to show more details: name, balance, address, manager.
            telegramMessages.manageGroupView().updMenuToManageGroupActionsMenu(callbackQuery, balance);
            telegramState.updateBalanceEdit(userState.getTelegramId(), telegramState
                    .getOrCreateBalanceEdit(userState.getTelegramId()).withSelectedBalanceId(balanceId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW));
        } else {
            logger.error("Group balance not found for ID: {}", balanceId);
        }
    }
}
