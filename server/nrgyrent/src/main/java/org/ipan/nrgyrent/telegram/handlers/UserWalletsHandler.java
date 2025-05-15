package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class UserWalletsHandler implements AppUpdateHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case WALLETS:
                handleWalletsState(userState, update);
                break;
            case ADD_WALLETS:
                handleAddWalletsState(userState, update);
                break;
        }
    }

    private void handleWalletsState(UserState userState, Update update) {
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


    private void handleAddWalletsState(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }

        String text = message.getText();

        if (WalletTools.isValidTronAddress(text)) {
            userWalletService.createWallet(
                    AddOrUpdateUserWalletCommand.builder()
                            .walletAddress(text)
                            .userId(userState.getTelegramId())
                            .build());
            // deleteMessage(message);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MAIN_MENU));
            telegramMessages.updMenuToAddWalletSuccessMenu(userState);
        }
        // TODO: send validation message to user
    }
}
