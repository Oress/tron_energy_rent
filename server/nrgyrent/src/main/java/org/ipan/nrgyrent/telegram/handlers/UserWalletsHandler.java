package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Optional;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.UserWalletRepo;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.AddWalletState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.WalletsViews;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@TransitionHandler
public class UserWalletsHandler {
    private final TelegramState telegramState;
    private final UserWalletService userWalletService;
    private final UserWalletRepo userWalletRepo;
    private final WalletsViews walletsViews;

    @MatchStates({
        @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.WALLETS),
        @MatchState(state = States.USER_WALLET_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.NEW_WALLET_PROMPT_ADDRESS, callbackData = InlineMenuCallbacks.GO_BACK)
    })
    public void viewUserWallets(UserState userState, Update update) {
        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        walletsViews.updMenuToWalletsMenu(wallets, update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.WALLETS));
    }

    @MatchState(state = States.WALLETS, callbackData = InlineMenuCallbacks.ADD_WALLETS)
    public void handleAddNewWallet(UserState userState, Update update) {
        walletsViews.updMenuToPromptWalletAddress(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.NEW_WALLET_PROMPT_ADDRESS));
    }

    @MatchState(state = States.WALLETS, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleDeleteWallet(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        if (data.startsWith(InlineMenuCallbacks.DELETE_WALLETS)) {
            String walletId = data.split(" ")[1];
            userWalletService
                    .deleteWallet(DeleteUserWalletCommand.builder().walletId(Long.parseLong(walletId)).build());
            walletsViews.updMenuToDeleteWalletSuccessMenu(callbackQuery);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.DELETE_WALLETS_SUCCESS));
        }
    }

    @MatchState(state = States.WALLETS, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleOpenWallet(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        if (data.startsWith(WalletsViews.OPEN_WALLET)) {
            String walletId = data.split(WalletsViews.OPEN_WALLET)[1];
            Optional<UserWallet> byId = userWalletRepo.findById(Long.parseLong(walletId));
            walletsViews.showWalletDetails(byId.get(), callbackQuery);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.USER_WALLET_PREVIEW));
        }
    }


    @MatchState(state = States.NEW_WALLET_PROMPT_ADDRESS, updateTypes = UpdateType.MESSAGE)
    public void handlePromptNewAddress(UserState userState, Update update) {
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        String text = message.getText();

        if (WalletTools.isValidTronAddress(text)) {
            AddWalletState addWalletState = telegramState.getOrCreateAddWalletState(userState.getTelegramId());
            telegramState.updateAddWalletState(userState.getTelegramId(), addWalletState.withAddress(text));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.NEW_WALLET_PROMPT_LABEL));
            walletsViews.updMenuToPromptWalletLabel(userState);
        }
        // TODO: send validation message to user
    }

    @MatchState(state = States.NEW_WALLET_PROMPT_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handlePromptNewLabel(UserState userState, Update update) {
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
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADD_WALLETS_SUCCESS));
        walletsViews.updMenuToAddWalletSuccessMenu(userState);
    }
}
