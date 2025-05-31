package org.ipan.nrgyrent.telegram.handlers;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.GroupSearchState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageGroupActionsView;
import org.ipan.nrgyrent.telegram.views.ManageGroupSearchView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@Slf4j
public class ManageGroupSearchHandler {
    private final int pageSize;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final BalanceRepo balanceRepo;
    private final ManageGroupActionsView manageGroupActionsView;

    public ManageGroupSearchHandler(
        @Value("${app.pagination.groups.page-size:20}") Integer pageSize,
        TelegramState telegramState,
        TelegramMessages telegramMessages,
        BalanceRepo balanceRepo,
        ManageGroupActionsView manageGroupActionsView) {
        this.pageSize = pageSize;
        this.telegramState = telegramState;
        this.telegramMessages = telegramMessages;
        this.balanceRepo = balanceRepo;
        this.manageGroupActionsView = manageGroupActionsView;
    }


    @MatchStates({
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_SEARCH),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_SEARCH_RESET)
    })
    public void resetSearch(UserState userState, Update update) {
        GroupSearchState searchState = telegramState.getOrCreateGroupSearchState(userState.getTelegramId());
        telegramState.updateGroupSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<Balance> nextPage = balanceRepo.findAllByTypeOrderById(BalanceType.GROUP, PageRequest.of(0, pageSize));
        telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(nextPage, userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_SEARCH));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_SEARCH, updateTypes = UpdateType.MESSAGE)
    public void searchGroupByLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Searching for groups with label: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            if (queryStr.length() < 3) {
                logger.info("Query string is too short: {}", queryStr);
                // telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(null,
                // message);
                return;
            }

            GroupSearchState searchState = telegramState.getOrCreateGroupSearchState(userState.getTelegramId());
            telegramState.updateGroupSearchState(userState.getTelegramId(), searchState.withQuery(queryStr));
            Page<Balance> firstPage = balanceRepo.findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType.GROUP,
                    queryStr, PageRequest.of(0, pageSize));
            telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(firstPage, userState);
        }
    }

    @MatchStates({
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_NEXT_PAGE)
    })
    public void nextPage(UserState userState, Update update) {
        GroupSearchState searchState = telegramState.getOrCreateGroupSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateGroupSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Balance> nextPage = balanceRepo.findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType.GROUP,
                    queryStr, PageRequest.of(pageNumber, pageSize));
        telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(nextPage, userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_SEARCH));
    }

    @MatchStates({
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_PREV_PAGE)
    })
    public void prevPage(UserState userState, Update update) {
        GroupSearchState searchState = telegramState.getOrCreateGroupSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateGroupSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Balance> prevPage = balanceRepo.findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType.GROUP,
                    queryStr, PageRequest.of(pageNumber, pageSize));
        telegramMessages.manageGroupSearchView().updMenuToManageGroupsSearchResult(prevPage, userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_SEARCH));
    }

    @MatchStates({
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openGroupFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        if (data.startsWith(ManageGroupSearchView.OPEN_BALANCE)) {
            String balanceIdStr = data.split(ManageGroupSearchView.OPEN_BALANCE)[1];
            Long balanceId = Long.parseLong(balanceIdStr);
            openGroupBalance(userState, balanceId);
        }
    }

    @MatchStates({
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_USERS_REVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_RENAMED_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_BALANCE_ADJUSTED_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_MANAGER, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_MANAGER_CHANGED_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SEARCHING, callbackData = InlineMenuCallbacks.GO_BACK),

    })
    public void openGroupFromBackBtn(UserState userState, Update update) {
        BalanceEdit balanceEdit = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());

        if (balanceEdit.getSelectedBalanceId() != null) {
            Long balanceId = balanceEdit.getSelectedBalanceId();
            openGroupBalance(userState, balanceId);
        }
    }

    @MatchStates({
        @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_GROUP),
        @MatchState(state = States.MANAGER_GROUPS_ACTION_ADD_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.MANAGER_GROUPS_ACTION_REMOVE_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.MANAGER_GROUP_VIEW_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.MANAGER_GROUPS_ACTION_ADD_USERS_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.MANAGER_GROUPS_ACTION_REMOVE_USERS_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void openGroupForManager(UserState userState, Update update) {
        Long managingGroupId = userState.getManagingGroupId();

        if (managingGroupId != null) {
            openGroupBalanceForManager(userState, managingGroupId);
        } else {
            logger.error("Selected balance ID is null for user: {}", userState.getTelegramId());
        }
    }

    public void openGroupBalance(UserState userState, Long balanceId) {
        Optional<Balance> groupBalance = balanceRepo.findById(balanceId);
        if (groupBalance.isPresent()) {
            Balance balance = groupBalance.get();
            manageGroupActionsView.updMenuToManageGroupActionsMenu(userState, balance);
            telegramState.updateBalanceEdit(userState.getTelegramId(), telegramState
                    .getOrCreateBalanceEdit(userState.getTelegramId()).withSelectedBalanceId(balanceId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW));
        } else {
            logger.error("Group balance not found for ID: {}", balanceId);
        }
    }

    public void openGroupBalanceForManager(UserState userState, Long balanceId) {
        Optional<Balance> groupBalance = balanceRepo.findById(balanceId);
        if (groupBalance.isPresent()) {
            Balance balance = groupBalance.get();
            manageGroupActionsView.updMenuToManageGroupActionsMenuForManager(userState, balance);
            telegramState.updateBalanceEdit(userState.getTelegramId(), telegramState
                    .getOrCreateBalanceEdit(userState.getTelegramId()).withSelectedBalanceId(balanceId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.MANAGER_GROUP_PREVIEW));
        } else {
            logger.error("Group balance not found for ID: {}", balanceId);
        }
    }
}
