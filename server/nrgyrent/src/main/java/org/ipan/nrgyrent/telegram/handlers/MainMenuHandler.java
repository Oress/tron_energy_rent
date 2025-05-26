package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.DepositViews;
import org.ipan.nrgyrent.telegram.views.HistoryViews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@TransitionHandler
public class MainMenuHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final OrderRepo orderRepo;

    private final DepositViews depositViews;
    private final HistoryViews historyViews;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.DEPOSIT)
    public void handleDeposit(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        depositViews.updMenuToDepositsMenu(userState, user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.HISTORY)
    public void handleTransactionHistory(UserState userState, Update update) {
        Page<Order> page = orderRepo.findAllByUserTelegramIdOrderByCreatedAtDesc(userState.getTelegramId(), PageRequest.of(0, 5));
        historyViews.updMenuToHistoryMenu(page.toList().reversed(), update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.HISTORY));
    }
}
