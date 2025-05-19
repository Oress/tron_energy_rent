package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.AddWalletState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.WalletsViews;
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
    private final WalletsViews walletsViews;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case WALLETS:
                handleWalletsState(userState, update);
                break;
            case NEW_WALLET_PROMPT_ADDRESS:
                handlePromptNewAddress(userState, update);
                break;
            case NEW_WALLET_PROMPT_LABEL:
                handlePromptNewLabel(userState, update);
                break;
        }
    }

    private void handleWalletsState(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.ADD_WALLETS.equals(data)) {
                walletsViews.updMenuToPromptWalletAddress(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.NEW_WALLET_PROMPT_ADDRESS));
            } else if (data.startsWith(InlineMenuCallbacks.DELETE_WALLETS)) {
                String walletId = data.split(" ")[1];
                userWalletService
                        .deleteWallet(DeleteUserWalletCommand.builder().walletId(Long.parseLong(walletId)).build());
                walletsViews.updMenuToDeleteWalletSuccessMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.DELETE_WALLETS_SUCCESS));
            }
        }
    }


    private void handlePromptNewAddress(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }

        String text = message.getText();

        if (WalletTools.isValidTronAddress(text)) {
            AddWalletState addWalletState = telegramState.getOrCreateAddWalletState(userState.getTelegramId());
            telegramState.updateAddWalletState(userState.getTelegramId(), addWalletState.withAddress(text));
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.NEW_WALLET_PROMPT_LABEL));
            walletsViews.updMenuToPromptWalletLabel(userState);
        }
        // TODO: send validation message to user
    }

    private void handlePromptNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }

        String text = message.getText();

        AddWalletState addWalletState = telegramState.getOrCreateAddWalletState(userState.getTelegramId());
        userWalletService.createWallet(
                AddOrUpdateUserWalletCommand.builder()
                        .walletAddress(addWalletState.getAddress())
                        .label(text)
                        .userId(userState.getTelegramId())
                        .build());
        // deleteMessage(message);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADD_WALLETS_SUCCESS));
        walletsViews.updMenuToAddWalletSuccessMenu(userState);
        // TODO: send validation message to user
    }
}
