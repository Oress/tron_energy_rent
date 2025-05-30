package org.ipan.nrgyrent.telegram;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.ipan.nrgyrent.telegram.views.ManageGroupSearchView;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
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
    public Message sendTransactionRefundNotification(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_ORDER_REFUNDED)
                .replyMarkup(getToMainMenuNotificationMarkup())
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendTransactionSuccessNotification(UserState userState, Balance balance) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(getSuccessfulTransactionMessage(balance))
                .replyMarkup(getToMainMenuNotificationMarkup())
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    private String getSuccessfulTransactionMessage(Balance balance) {
        return """
                ✅ Транзакция успешно завершена

                *Ваш баланс: %s TRX*
                """.formatted(FormattingTools.formatBalance(balance.getSunBalance()));
    }

    @SneakyThrows
    public void sendWithdrawalSuccessful(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_WITHDRWAL_SUCCESS)
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalFail(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_WITHDRWAL_FAIL)
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendTopupNotification(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(StaticLabels.NTFN_BALANCE_TOPUP)
                .replyMarkup(getOkNotificationMarkup())
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
    public Message sendMainMenu(UserState userState, Long chatId, AppUser user) {
        Tariff tariff = user.getBalance().getTariff();

        if (tariff == null) {
            logger.error("User {} has no tariff set", user.getTelegramUsername());
        }

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(getMainMenuMessage(user))
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), false, tariff))
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public Message sendAdminMainMenu(UserState userState, Long chatId, AppUser user) {
        Tariff tariff = user.getBalance().getTariff();

        if (tariff == null) {
            logger.error("User {} has no tariff set", user.getTelegramUsername());
        }

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .text(getMainMenuMessage(user))
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), true, tariff))
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public void updateMsgToMainMenu(UserState userState, AppUser user) {
        Tariff tariff = user.getBalance().getTariff();

        if (tariff == null) {
            logger.error("User {} has no tariff set", user.getTelegramUsername());
        }

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getMainMenuMessage(user))
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .parseMode("MARKDOWN")
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), false, tariff))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updateMsgToAdminMainMenu(UserState userState, CallbackQuery callbackQuery, AppUser user) {
        Tariff tariff = user.getBalance().getTariff();

        if (tariff == null) {
            logger.error("User {} has no tariff set", user.getTelegramUsername());
        }

        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getMainMenuMessage(user))
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), true, tariff))
                .build();
        tgClient.execute(message);
    }

    private String getMainMenuMessage(AppUser user) {
        Balance personalBalance = user.getBalance();
        Balance groupBalance = user.getGroupBalance();

        return """
            ⚡ Приветствуем в нашем сервисе ⚡

            Выберите действие, нажав кнопку ниже, время аренды - 1 час

            *Ваш баланс: %s TRX* %s

            [@FlashTronRent_support](https://t.me/FlashTronRent_support) - поможет и ответит на все вопросы
            """.formatted(FormattingTools.formatBalance(personalBalance.getSunBalance()),
                groupBalance != null 
                    ? "\n*Баланс группы: %s TRX*".formatted(FormattingTools.formatBalance(groupBalance.getSunBalance()))
                    : ""
                );
    }

    private InlineKeyboardMarkup getMainMenuReplyMarkup(Boolean isManager, Boolean isAdmin, Tariff tariff) {
        var builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(getFirstTransactionTypeLabel(tariff.getTransactionType1AmountSun()))
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_65k)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(getSecondTransactionTypeLabel(tariff.getTransactionType2AmountSun()))
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
                                        .text(StaticLabels.WITHDRAW_TRX)
                                        .callbackData(InlineMenuCallbacks.WITHDRAW_TRX)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_HISTORY)
                                        .callbackData(InlineMenuCallbacks.HISTORY)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_WALLETS)
                                        .callbackData(InlineMenuCallbacks.WALLETS)
                                        .build()));

        if (isManager) {
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_MANAGE_GROUP)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUP)
                                        .build()));
        }

        if (isAdmin) {
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.MENU_ADMIN)
                                        .callbackData(InlineMenuCallbacks.ADMIN_MENU)
                                        .build()));
        }
        return builder.build();
    }

    private String getFirstTransactionTypeLabel(Long trxAmount) {
        return "⚡ 1 тр на кош с USDT (%s TRX)".formatted(FormattingTools.formatBalance(trxAmount));
    }

    private String getSecondTransactionTypeLabel(Long trxAmount) {
        return "⚡ 1 тр на кош без USDT или биржу (%s TRX)".formatted(FormattingTools.formatBalance(trxAmount));
    }

    private InlineKeyboardMarkup getToMainMenuNotificationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build())

                )
                .build();
    }

    private InlineKeyboardMarkup getOkNotificationMarkup() {
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
}
