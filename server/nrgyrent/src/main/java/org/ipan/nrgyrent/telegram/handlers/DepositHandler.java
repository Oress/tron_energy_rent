package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DepositHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;

    public void handleUpdate(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            if (InlineMenuCallbacks.ADD_WALLETS.equals(data)) {
                telegramMessages.updMenuToAddWalletsMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADD_WALLETS));
            } else if (data.startsWith(InlineMenuCallbacks.DELETE_WALLETS)) {
                String walletId = data.split(" ")[1];
                userWalletService
                        .deleteWallet(DeleteUserWalletCommand.builder().walletId(Long.parseLong(walletId)).build());
                telegramMessages.updMenuToDeleteWalletSuccessMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.DELETE_WALLETS_SUCCESS));
            }
        }
    }
}
