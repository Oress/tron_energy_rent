package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.DepositViews;
import org.ipan.nrgyrent.telegram.views.HistoryViews;
import org.ipan.nrgyrent.telegram.views.TransactionsViews;
import org.ipan.nrgyrent.telegram.views.WalletsViews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MainMenuHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final UserWalletService userWalletService;
    private final UserService userService;
    private final OrderRepo orderRepo;

    private final WalletsViews walletsViews;
    private final DepositViews depositViews;
    private final AdminViews adminViews;
    private final HistoryViews historyViews;
    private final TransactionsViews transactionsViews;


    @Override
    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.TRANSACTION_65k.equals(data)) {
                proceedToTransactions(userState, callbackQuery, AppConstants.ENERGY_65K);
            } else if (InlineMenuCallbacks.TRANSACTION_131k.equals(data)) {
                proceedToTransactions(userState, callbackQuery, AppConstants.ENERGY_131K);
            } else if (InlineMenuCallbacks.DEPOSIT.equals(data)) {
                AppUser user = userService.getById(userState.getTelegramId());
                depositViews.updMenuToDepositsMenu(callbackQuery, user);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
            } else if (InlineMenuCallbacks.WALLETS.equals(data)) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                walletsViews.updMenuToWalletsMenu(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.WALLETS));
            } else if (InlineMenuCallbacks.ADMIN_MENU.equals(data)) {
                // TODO: extra validation here ??
                adminViews.updMenuToAdminMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MENU));
            } else if (InlineMenuCallbacks.HISTORY.equals(data)) {
                Page<Order> page = orderRepo.findAllByUserTelegramIdOrderByCreatedAtDesc(userState.getTelegramId(), PageRequest.of(0, 5));
                historyViews.updMenuToHistoryMenu(page.toList().reversed(), callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.HISTORY));
            }
        }
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

            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PROMPT_WALLET));
        } else {
            transactionsViews.updMenuToPromptBalanceType(callbackQuery);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PROMPT_BALANCE_TYPE));
        }
        telegramState.updateTransactionParams(userState.getTelegramId(), transactionParams.withGroupBalance(useGroupBalance).withEnergyAmount(energyAmount));
    }
}
