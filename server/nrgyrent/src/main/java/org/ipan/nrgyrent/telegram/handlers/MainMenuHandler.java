package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.DepositViews;
import org.ipan.nrgyrent.telegram.views.HistoryViews;
import org.ipan.nrgyrent.telegram.views.TransactionsViews;
import org.ipan.nrgyrent.telegram.views.WalletsViews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@TransitionHandler
public class MainMenuHandler {
    private final TelegramState telegramState;
    private final UserWalletService userWalletService;
    private final UserService userService;
    private final OrderRepo orderRepo;

    private final WalletsViews walletsViews;
    private final DepositViews depositViews;
    private final AdminViews adminViews;
    private final HistoryViews historyViews;
    private final TransactionsViews transactionsViews;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_65k)
    public void handleTransaction65k(UserState userState, Update update) {
        proceedToTransactions(userState, update.getCallbackQuery(), AppConstants.ENERGY_65K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_131k)
    public void handleTransaction131k(UserState userState, Update update) {
        proceedToTransactions(userState, update.getCallbackQuery(), AppConstants.ENERGY_131K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.DEPOSIT)
    public void handleDeposit(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        depositViews.updMenuToDepositsMenu(update.getCallbackQuery(), user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.WALLETS)
    public void handleWallets(UserState userState, Update update) {
        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        walletsViews.updMenuToWalletsMenu(wallets, update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.WALLETS));
    }

    // TODO: extra validation here ??
    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.ADMIN_MENU)
    public void handleAdminMenu(UserState userState, Update update) {
        adminViews.updMenuToAdminMenu(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MENU));
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.HISTORY)
    public void handleTransactionHistory(UserState userState, Update update) {
        Page<Order> page = orderRepo.findAllByUserTelegramIdOrderByCreatedAtDesc(userState.getTelegramId(), PageRequest.of(0, 5));
        historyViews.updMenuToHistoryMenu(page.toList().reversed(), update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.HISTORY));
    }

    private void proceedToTransactions(UserState userState, CallbackQuery callbackQuery, Integer energyAmount) {
        AppUser byId = userService.getById(userState.getTelegramId());
        // If no group balance, proceed to
        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
        boolean useGroupBalance = true;
        if (byId.getGroupBalance() == null) {
            useGroupBalance = false;
            List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());

            if (energyAmount == AppConstants.ENERGY_131K) {
                transactionsViews.updMenuToTransaction131kMenu(wallets, callbackQuery);
            } else {
                transactionsViews.updMenuToTransaction65kMenu(wallets, callbackQuery);
            }

            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.TRANSACTION_PROMPT_WALLET));
        } else {
            transactionsViews.updMenuToPromptBalanceType(callbackQuery);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.TRANSACTION_PROMPT_BALANCE_TYPE));
        }
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams.withGroupBalance(useGroupBalance).withEnergyAmount(energyAmount));
    }
}
