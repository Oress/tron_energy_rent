package org.ipan.nrgyrent.telegram;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.ipan.nrgyrent.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.controller.WalletService;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.jetbrains.annotations.NotNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.web3j.crypto.WalletUtils;

import java.util.concurrent.ConcurrentHashMap;

// TODO: transtions
// TODO: caching state
// TODO: persisting state
@Component
@AllArgsConstructor
public class RentEnergyBot implements LongPollingSingleThreadUpdateConsumer {
    public static final String START = "/start";

    private TelegramClient tgClient;
    private WalletService walletService;

    private final ConcurrentHashMap<Long, UserState> userStateMap = new ConcurrentHashMap<>();

    @Override
    public void consume(Update update) {
        User from = getFrom(update);
        if (from.getIsBot()) {
            // Ignore bots
            return;
        }

        Long userId = from.getId();
        UserState userState = initUserState(userId);

        switch (userState.getCurrentState()) {
            case START:
                handleStartState(userState, update);
                break;
            case MAIN_MENU:
                handleMainMenu(userState, update);
                break;
            case ADD_WALLETS:
                handleAddWalletsState(userState, update);
                break;
            case TRANSACTION_65k:
                handleTransaction65kState(userState, update);
                break;
            case TRANSACTION_131k:
                break;
        }

        Message message = update.getMessage();
        CallbackQuery callbackQuery = update.getCallbackQuery();

        if (message != null && message.hasText()) {
            String text = message.getText();
            System.out.println("Message text: " + text);
            deleteMessage(message);
        } else if (callbackQuery != null) {
            String data = callbackQuery.getData();

            System.out.println("Callback query data: " + data);

            EditMessageText.builder().replyMarkup(InlineKeyboardMarkup.builder().build());
            if (InlineMenuCallbacks.TO_MAIN_MENU.equals(data)) {
                updateMsgToMainMenu(callbackQuery);
            }
        }
    }

    private void handleTransaction65kState(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            // Use callbackQuery.getData() to determine the wallet selected
            String data = callbackQuery.getData();
            System.out.println("Callback query data: " + data);
        }


        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();
            deleteMessage(message);
            if (WalletTools.isValidTronAddress(text)) {
                // send transaction
            } else {
                // warn user
            }
        }
    }

    private void handleMainMenu(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            System.out.println("Callback query data: " + data);

            if (InlineMenuCallbacks.TRANSACTION_65k.equals(data)) {
                updMenuToTransaction65kMenu(callbackQuery);
                userState.setCurrentState(States.TRANSACTION_65k);
            } else if (InlineMenuCallbacks.TRANSACTION_131k.equals(data)) {
                updMenuToTransaction131kMenu(callbackQuery);
                userState.setCurrentState(States.TRANSACTION_131k);
            } else if (InlineMenuCallbacks.DEPOSIT.equals(data)) {
//                sendDeposit(callbackQuery);
                userState.setCurrentState(States.DEPOSIT);
            } else if (InlineMenuCallbacks.WALLETS.equals(data)) {
                updMenuToWalletsMenu(callbackQuery);
                userState.setCurrentState(States.WALLETS);
            } else if (InlineMenuCallbacks.ADD_WALLETS.equals(data)) {
                updMenuToAddWalletsMenu(callbackQuery);
                userState.setCurrentState(States.ADD_WALLETS);
            }
        }
    }

    private void handleAddWalletsState(UserState userState, Update update) {
        Message message = update.getMessage();
        String text = message.getText();

        if (WalletTools.isValidTronAddress(text)) {
            // assuming valid address
            walletService.createWallet(
                    AddOrUpdateUserWalletCommand.builder()
                            .walletAddress(text)
                            .build()
            );
            deleteMessage(message);
            updMenuToAddWalletSuccessMenu(userState, update.getCallbackQuery());
        }
    }

    private void handleStartState(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            String text = message.getText();

            if (START.equals(text)) {
                sendMainMenu(userState, update.getMessage().getChatId());
            }
        }
    }

    @SneakyThrows
    private void deleteMessage(Message message) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
        tgClient.execute(deleteMessage);
    }

    @SneakyThrows
    private void updateMsgToMainMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getMainMenuReplyMarkup())
                .build();
        tgClient.execute(message);
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


    @Retryable
    private void sendMainMenu(UserState userState, Long chatId) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getMainMenuReplyMarkup())
                .build();
        try {
            Message execute = tgClient.execute(message);
            userState.setCurrentState(States.MAIN_MENU);
            userState.setMenuMessageId(execute.getMessageId());
            userState.setChatId(execute.getChatId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Retryable
    @SneakyThrows
    private void updMenuToTransaction65kMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_TRANSACTION_65K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    private void updMenuToTransaction131kMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_TRANSACTION_131K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    private void updMenuToWalletsMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_WALLETS)
                .replyMarkup(getWalletsMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    private void updMenuToAddWalletsMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_ADD_WALLET)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    private void updMenuToAddWalletSuccessMenu(UserState userState, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_ADD_WALLET_SUCCESS)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    private @NotNull UserState initUserState(Long userId) {
        return userStateMap.computeIfAbsent(userId, k -> {
            UserState newUserState = new UserState();
            newUserState.setTelegramId(userId);
            newUserState.setCurrentState(States.START);
            return newUserState;
        });
    }

    private InlineKeyboardMarkup getMainMenuReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_TRANSFER_ENERGY_65K)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_65k)
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_TRANSFER_ENERGY_131K)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_131k)
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_DEPOSIT)
                                        .callbackData(InlineMenuCallbacks.DEPOSIT)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_WALLETS)
                                        .callbackData(InlineMenuCallbacks.WALLETS)
                                        .build()
                        )

                )
                .build();
    }

    private InlineKeyboardMarkup getTransactionsMenuMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Wallet 1")
                                        .callbackData("Wallet 1")
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Wallet 2")
                                        .callbackData("Wallet 2")
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()
                        )

                )
                .build();
    }

    private InlineKeyboardMarkup getWalletsMenuMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.WLT_ADD_WALLET)
                                        .callbackData(InlineMenuCallbacks.ADD_WALLETS)
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Wallet 1")
                                        .callbackData("Wallet 1")
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.WLT_DELETE_WALLET)
                                        .callbackData("delete_wallet 1")
                                        .build()
                        )
                )
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()
                        )

                )
                .build();
    }

    private InlineKeyboardMarkup getToMainMenuMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()
                        )

                )
                .build();
    }

}
