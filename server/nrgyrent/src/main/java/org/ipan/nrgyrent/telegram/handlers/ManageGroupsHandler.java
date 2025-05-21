package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@TransitionHandler
@AllArgsConstructor
public class ManageGroupsHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final BalanceRepo balanceRepo;

    private final ManageGroupNewGroupView manageGroupNewGroupView;

    @MatchStates({
        @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_GROUPS),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void showManageGroupsMenu(UserState userState, Update update) {
        manageGroupNewGroupView.updMenuToManageGroupsMenu(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_SEARCH)
    public void searchGroups(UserState userState, Update update) {
        Page<Balance> firstPage = balanceRepo.findAllByTypeOrderById(BalanceType.GROUP, PageRequest.of(0, 10));
        telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(firstPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_SEARCH));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ADD)
    public void startAddGroup(UserState userState, Update update) {
        telegramMessages.manageGroupView().updMenuToManageGroupsAddPromptLabel(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL));
    }
}
