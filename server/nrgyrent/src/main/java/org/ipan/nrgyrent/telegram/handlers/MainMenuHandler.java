package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.DepositViews;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class MainMenuHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;
    private final UserService userService;

    private final DepositViews depositViews;


    @Override
    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.TRANSACTION_65k.equals(data)) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                telegramMessages.updMenuToTransaction65kMenu(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_65k));
            } else if (InlineMenuCallbacks.TRANSACTION_131k.equals(data)) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                telegramMessages.updMenuToTransaction131kMenu(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_131k));
            } else if (InlineMenuCallbacks.DEPOSIT.equals(data)) {
                AppUser user = userService.getById(userState.getTelegramId());
                depositViews.updMenuToDepositsMenu(callbackQuery, user);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
            } else if (InlineMenuCallbacks.WALLETS.equals(data)) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                telegramMessages.updMenuToWalletsMenu(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.WALLETS));
            } else if (InlineMenuCallbacks.ADMIN_MENU.equals(data)) {
                // TODO: extra validation here ??
                telegramMessages.updMenuToAdminMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MENU));
            }
        }
    }
}
