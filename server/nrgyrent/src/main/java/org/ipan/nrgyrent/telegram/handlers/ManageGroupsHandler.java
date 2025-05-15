package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ManageGroupsHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final BalanceRepo balanceRepo;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.MANAGE_GROUPS_SEARCH.equals(data)) {
                Page<Balance> firstPage = balanceRepo.findAllByTypeOrderById(BalanceType.GROUP, PageRequest.of(0, 10));
                telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(firstPage, userState);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS_SEARCH));
            } else if (InlineMenuCallbacks.MANAGE_GROUPS_ADD.equals(data)) {
                telegramMessages.manageGroupView().updMenuToManageGroupsAddPromptLabel(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL));
            }
        }
    }
}
