package org.ipan.nrgyrent.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class RentEnergyBot implements LongPollingSingleThreadUpdateConsumer {
    public static final String START = "/start";
    public static final int WAIT_FOR_CALLBACK = 10;
    public static final int ITRX_OK_CODE = 0;

    private TelegramState telegramState;
    private TelegramMessages telegramMessages;
    private UserWalletService userWalletService;
    private UserService userService;
    private ItrxService itrxService;
    private OrderService orderService;


    @Override
    public void consume(Update update) {
        User from = getFrom(update);
        if (from.getIsBot()) {
            // Ignore bots
            return;
        }

        Long userId = from.getId();
        UserState userState = telegramState.getOrCreateUserState(userId);

        handleStartState(userState, update);

        tryRemoveNotification(userState, update);

        switch (userState.getState()) {
            case MAIN_MENU:
                handleMainMenu(userState, update);
                break;
            case WALLETS:
                handleWalletsState(userState, update);
                break;
            case ADD_WALLETS:
                handleAddWalletsState(userState, update);
                break;
            case TRANSACTION_65k:
                handleTransaction65kState(userState, update);
                break;
            case TRANSACTION_131k:
                handleTransaction131kState(userState, update);
                break;
        }

        Message message = update.getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (message != null && message.hasText()) {
            telegramMessages.deleteMessage(message);
        } else if (callbackQuery != null) {
            String data = callbackQuery.getData();

            EditMessageText.builder().replyMarkup(InlineKeyboardMarkup.builder().build());
            if (InlineMenuCallbacks.TO_MAIN_MENU.equals(data)) {
                telegramMessages.updateMsgToMainMenu(callbackQuery);
                telegramState.updateUserState(userId, userState.withState(States.MAIN_MENU));
            }
        }
    }

    private void tryRemoveNotification(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null && InlineMenuCallbacks.NTFN_OK.equals(callbackQuery.getData())) {
            telegramMessages.deleteMessage(userState, callbackQuery);
        }
    }

    private void handleTransaction65kState(UserState userState, Update update) {
        handletTransactionState(userState, update, 65_000);
    }

    private void handleTransaction131kState(UserState userState, Update update) {
        handletTransactionState(userState, update, 131_000);
    }

    private void handletTransactionState(UserState userState, Update update, Integer energyAmount) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmount, callbackQuery.getData());
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, energyAmount, message.getText());
        }
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmount, String walletAddress) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            telegramMessages.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(energyAmount, walletAddress, correlationId);

            // Waiting WAIT_FOR_CALLBACK seconds for callback from itrx
            // if callback is not received, enqueue the request and notify the user
            // otherwise, update the menu to transaction success
            if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                return;
                // TODO: do something here
            }

            orderService.createPendingOrder(
                    AddOrUpdateOrderCommand.builder()
                            .userId(userState.getTelegramId())
                            .receiveAddress(walletAddress)
                            .energyAmount(energyAmount)
                            .correlationId(correlationId.toString())
                            .serial(placeOrderResponse.getSerial())
                            .build()
            );
            OrderCallbackRequest orderCallbackRequest = itrxService.getCorrelatedCallbackRequest(correlationId, WAIT_FOR_CALLBACK);

            if (orderCallbackRequest != null) {
                telegramMessages.updMenuToTransactionSuccess(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_SUCCESS));
                // TODO: add SUCCESSFUL DB record for transaction
            } else {
                telegramMessages.updMenuToTransactionPending(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PENDING));
                // TODO: add PENDING DB record for transaction
            }
        }
    }

    private void handleMainMenu(UserState userState, Update update) {
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
//                sendDeposit(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
            } else if (InlineMenuCallbacks.WALLETS.equals(data)) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                telegramMessages.updMenuToWalletsMenu(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.WALLETS));
            }
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
                userWalletService.deleteWallet(DeleteUserWalletCommand.builder().walletId(Long.parseLong(walletId)).build());
                telegramMessages.updMenuToDeleteWalletSuccessMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DELETE_WALLETS_SUCCESS));
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
                            .build()
            );
//            deleteMessage(message);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MAIN_MENU));
            telegramMessages.updMenuToAddWalletSuccessMenu(userState);
        }
        // TODO: send validation message to user
    }

    private void handleStartState(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();

            if (START.equals(text)) {
                Message newMenuMsg = telegramMessages.sendMainMenu(update.getMessage().getChatId());
                telegramState.updateUserState(userState.getTelegramId(), userState
                        .withState(States.MAIN_MENU)
                        .withChatId(newMenuMsg.getChatId())
                        .withMenuMessageId(newMenuMsg.getMessageId())
                );
                userService.createUser(
                        CreateUserCommand.builder()
                                .telegramId(userState.getTelegramId())
                                .build()
                );
            }
        }
    }

    private User getFrom(Update update) {
        Message message = update.getMessage();
        if (message != null && message.getFrom() != null) {
            return message.getFrom();
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null && callbackQuery.getFrom() != null) {
            return callbackQuery.getFrom();
        }

        // TODO: make custom exception for this.
        throw new IllegalStateException("Cannot determine user from update: " + update);
    }

}
