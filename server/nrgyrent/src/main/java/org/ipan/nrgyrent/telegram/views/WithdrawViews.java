package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class WithdrawViews {
    public static final String NTFN_WITHDRWAL_FAIL = "❌ Вывод средств не удался";
    public static final String NTFN_WITHDRWAL_SUCCESS = """
            ✅ Вывод средств успешно завершен
            Средства были переведены на ваш кошелек
            """;
    private static final String NTFN_WITHDRWAL_FAIL_NOT_ENOUGH_BALANCE = """
            ❌ Ошибка вывода средств

            У вас недостаточно средств для вывода. Пожалуйста, проверьте баланс и попробуйте снова.
            """;
    private static final String NTFN_WITHDRWAL_FAIL_SERVICE_NOT_ENOUGH_BALANCE = """
            ❌ Ошибка вывода средств

            На сервисе сейчас недостаточно средств для вывода. Пожалуйста, попробуйте позже.
            """;

    private static final String MSG_WITHDRAW_PROMPT_BALANCE_TYPE = """
            💰 Вывод средств

            Пожалуйста, выберите тип баланса, с которого вы хотите вывести средства.
            """;

    private static final String MSG_WITHDRAW_TRX = """
            💰 Вывод TRX

            Выберите кошелек, на который хотите вывести TRX или введите адрес кошелька, на который хотите вывести средства.
            """;

    private static final String MSG_WITHDRAW_TRX_IN_PROGRESS = """
            💰 Вывод TRX

            Вывод средств в процессе. Вам будет отправлено уведомление, когда средства будут выведены.
            """;

    private static final String LBL_WITHDRAWAL_PERSONAL_BALANCE = "Личный баланс";
    private static final String LBL_WITHDRAWAL_GROUP_BALANCE = "Груповой баланс";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void withdrawTrxInactiveWallet(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Ошибка вывода средств\n\n" +
                        "Кошелек неактивен. Пожалуйста, выберите другой кошелек или активируйте текущий.")
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void withdrawTrxInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_WITHDRAW_TRX_IN_PROGRESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalSuccessful(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(NTFN_WITHDRWAL_SUCCESS)
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalFailNotEnoughBalance(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(NTFN_WITHDRWAL_FAIL_NOT_ENOUGH_BALANCE)
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalFailServiceNotEnoughBalance(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(NTFN_WITHDRWAL_FAIL_SERVICE_NOT_ENOUGH_BALANCE)
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalFail(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(NTFN_WITHDRWAL_FAIL)
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptBalanceType(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_WITHDRAW_PROMPT_BALANCE_TYPE)
                .replyMarkup(getWithdrawBalanceMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptAmountAgainNotEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getPromptAmountForWithdrawalNotEnoughBalance())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptAmount(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getPromptAmountForWithdrawal())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptWallets(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_WITHDRAW_TRX)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
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

    private InlineKeyboardMarkup getWithdrawBalanceMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(LBL_WITHDRAWAL_PERSONAL_BALANCE)
                                        .callbackData(InlineMenuCallbacks.WITHDRAW_BALANCE_PERSONAL)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(LBL_WITHDRAWAL_GROUP_BALANCE)
                                        .callbackData(InlineMenuCallbacks.WITHDRAW_BALANCE_GROUP)
                                        .build())

                )
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

    private InlineKeyboardMarkup getTransactionsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(wallet.getLabel())
                            .callbackData(wallet.getAddress())
                            .build());
            return row;
        }).toList();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        walletRows.forEach(builder::keyboardRow);

        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getPromptAmountForWithdrawal() {
        return """
                💰 Вывод средств

                ❗️Коммиссия за вывод средств составляет 1 TRX.

                ❗️Минимальная сумма для вывода составляет 10 TRX.


                Пожалуйста, введите сумму, которую вы хотите вывести.
                """;
    }

    private String getPromptAmountForWithdrawalNotEnoughBalance() {
        return """
                💰 Вывод средств

                У вас недостаточно средств для вывода такой суммы.
                Пожалуйста, введите сумму, которую вы хотите вывести.
                """;
    }
}
