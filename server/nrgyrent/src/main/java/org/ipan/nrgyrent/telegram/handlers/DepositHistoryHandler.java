package org.ipan.nrgyrent.telegram.handlers;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.DepositHistoryItem;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.DepositSearchState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.DepositTransactionsSearchView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.Update;

@TransitionHandler
@Slf4j
public class DepositHistoryHandler {
    private final int pageSize;
    private final TelegramState telegramState;
    private final DepositTransactionsSearchView depositTransactionsSearchView;
    private final DepositTransactionRepo depositTransactionRepo;
    private final UserService userService;

    public DepositHistoryHandler(@Value("${app.pagination.tariffs.page-size:20}") int pageSize,
                                 TelegramState telegramState,
                                 DepositTransactionsSearchView depositTransactionsSearchView,
                                 UserService userService,
                                 DepositTransactionRepo depositTransactionRepo
    ) {
//        this.pageSize = pageSize;
        this.pageSize = 2;
        this.telegramState = telegramState;
        this.depositTransactionsSearchView = depositTransactionsSearchView;
        this.depositTransactionRepo = depositTransactionRepo;
        this.userService = userService;
    }

    @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.DEPOSIT_HISTORY)
    public void startChangeRefProgram(UserState userState, Update update) {
        AppUser byId = userService.getById(userState.getTelegramId());
        Balance balanceToUse = byId.getBalanceToUse();

        DepositSearchState searchState = telegramState.getOrCreateDepositSearchState(userState.getTelegramId());
        telegramState.updateDepositSearchState(userState.getTelegramId(), searchState.withCurrentPage(0));

        Page<DepositHistoryItem> nextPage = depositTransactionRepo.findAllByByBalanceId(balanceToUse.getId(), PageRequest.of(0, pageSize));
        depositTransactionsSearchView.updMenuToSearchResult(nextPage, userState);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT_HISTORY_SEARCHING));
    }

    @MatchStates({
            @MatchState(state = States.DEPOSIT_HISTORY_SEARCHING, callbackData = InlineMenuCallbacks.DEPOSIT_NEXT_PAGE)
    })
    public void nextPageRefProgram(UserState userState, Update update) {
        AppUser byId = userService.getById(userState.getTelegramId());
        Balance balanceToUse = byId.getBalanceToUse();

        DepositSearchState searchState = telegramState.getOrCreateDepositSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        telegramState.updateDepositSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<DepositHistoryItem> nextPage = depositTransactionRepo.findAllByByBalanceId(balanceToUse.getId(), PageRequest.of(pageNumber, pageSize));
        depositTransactionsSearchView.updMenuToSearchResult(nextPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.DEPOSIT_HISTORY_SEARCHING, callbackData = InlineMenuCallbacks.DEPOSIT_PREV_PAGE)
    })
    public void prevPageRefProgram(UserState userState, Update update) {
        AppUser byId = userService.getById(userState.getTelegramId());
        Balance balanceToUse = byId.getBalanceToUse();

        DepositSearchState searchState = telegramState.getOrCreateDepositSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        telegramState.updateDepositSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<DepositHistoryItem> prevPage = depositTransactionRepo.findAllByByBalanceId(balanceToUse.getId(), PageRequest.of(pageNumber, pageSize));
        depositTransactionsSearchView.updMenuToSearchResult(prevPage, userState);
    }
}
