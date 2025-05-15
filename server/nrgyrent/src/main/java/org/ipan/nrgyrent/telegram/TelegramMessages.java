package org.ipan.nrgyrent.telegram;

import java.util.List;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.ipan.nrgyrent.telegram.views.ManageGroupSearchView;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class TelegramMessages {
    private TelegramClient tgClient;
    private ManageGroupNewGroupView manageGroupView;
    private ManageGroupSearchView manageGroupSearchView;

    public ManageGroupNewGroupView manageGroupView() {
        return manageGroupView;
    }

    public ManageGroupSearchView manageGroupSearchView() {
        return manageGroupSearchView;
    }

    @SneakyThrows
    public void sendTransactionSuccessNotification(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_ORDER_SUCCESS)
                .replyMarkup(getOrderSuccessNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendTransactionRefundNotification(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_ORDER_REFUNDED)
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        tgClient.execute(deleteMessage);
    }

    @SneakyThrows
    public void deleteMessages(Long chatId, List<Integer> messageIds) {
        DeleteMessages deleteMessages = DeleteMessages
                .builder()
                .chatId(chatId)
                .messageIds(messageIds)
                .build();
        tgClient.execute(deleteMessages);
    }

    @SneakyThrows
    public void deleteMessage(Message message) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
        tgClient.execute(deleteMessage);
    }

    @SneakyThrows
    public void deleteMessage(UserState userState, CallbackQuery callbackQuery) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(userState.getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build();
        tgClient.execute(deleteMessage);
    }

    @Retryable
    @SneakyThrows
    public Message sendMainMenu(Long chatId) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getMainMenuReplyMarkup())
                .build();
        return tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public Message sendAdminMainMenu(Long chatId) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getAdminMainMenuReplyMarkup())
                .build();
        return tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransaction65kMenu(List<UserWallet> wallets, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_TRANSACTION_65K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransactionInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_TRANSACTION_PROGRESS)
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransactionSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_TRANSACTION_SUCCESS)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransactionPending(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_TRANSACTION_PENDING)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransaction131kMenu(List<UserWallet> wallets, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_TRANSACTION_131K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToDepositsMenu(CallbackQuery callbackQuery, String walletAddress, Long sunBalance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.getDepositMenuText(walletAddress, sunBalance))
                .parseMode("MARKDOWN")
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToWalletsMenu(List<UserWallet> wallets, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_WALLETS)
                .replyMarkup(getWalletsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAdminMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_ADMIN_MENU)
                .replyMarkup(getAdminMenuReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAddWalletsMenu(CallbackQuery callbackQuery) {
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
    public void updMenuToDeleteWalletSuccessMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_DELETE_WALLET_SUCCESS)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAddWalletSuccessMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_ADD_WALLET_SUCCESS)
                .replyMarkup(getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updateMsgToMainMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getMainMenuReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updateMsgToAdminMainMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(StaticLabels.MSG_MAIN_MENU_TEXT)
                .replyMarkup(getAdminMainMenuReplyMarkup())
                .build();
        tgClient.execute(message);
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
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_TRANSFER_ENERGY_131K)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_131k)
                                        .build()))
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
                                        .build())

                )
                .build();
    }

    private InlineKeyboardMarkup getAdminMenuReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_ADMIN_MANAGE_GROUPS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_ADMIN_MANAGE_USERS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup getAdminMainMenuReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_TRANSFER_ENERGY_65K)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_65k)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_TRANSFER_ENERGY_131K)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_131k)
                                        .build()))
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
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_ADMIN)
                                        .callbackData(InlineMenuCallbacks.ADMIN_MENU)
                                        .build()))
                .build();
    }





    private InlineKeyboardMarkup getOrderSuccessNotificationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.OK)
                                        .callbackData(InlineMenuCallbacks.NTFN_OK)
                                        .build())

                )
                .build();
    }

    private InlineKeyboardMarkup getOrderRefundedNotificationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.OK)
                                        .callbackData(InlineMenuCallbacks.NTFN_OK)
                                        .build())

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
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup getWalletsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(WalletTools.formatTronAddress(wallet.getAddress()))
                            .callbackData(wallet.getId().toString())
                            .build(),
                    InlineKeyboardButton
                            .builder()
                            .text(StaticLabels.WLT_DELETE_WALLET)
                            .callbackData("delete_wallet " + wallet.getId().toString())
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.WLT_ADD_WALLET)
                                        .callbackData(InlineMenuCallbacks.ADD_WALLETS)
                                        .build()));
        walletRows.forEach(builder::keyboardRow);

        return builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(StaticLabels.TO_MAIN_MENU)
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build())

        )
                .build();
    }

    private InlineKeyboardMarkup getTransactionsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(WalletTools.formatTronAddress(wallet.getAddress()))
                            .callbackData(wallet.getAddress())
                            .build());
            return row;
        }).toList();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        walletRows.forEach(builder::keyboardRow);

        return builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(StaticLabels.TO_MAIN_MENU)
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build())

        )
                .build();
    }


}
