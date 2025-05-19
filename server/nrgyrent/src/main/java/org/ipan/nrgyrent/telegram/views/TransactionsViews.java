package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class TransactionsViews {
    public static final String LBL_TRANSACTION_BALANCE_PERSONAL = "Личный баланс";
    public static final String LBL_TRANSACTION_BALANCE_GROUP = "Групповой баланс";

    private static final String MSG_TRANSACTION_PROMPT_BALANCE_TYPE = "Пожалуйста, выберите тип баланса для транзакции:";

    private static final String MSG_TRANSACTION_65K_TEXT = """
            ⚡ Транзакции (1 тр на кош с USDT, 5.5 TRX)

            👇 Пожалуйста, выберите кошелек, для которого вы желаете перевести энергию 👇
            """;

    private static final String MSG_TRANSACTION_131K_TEXT = """
            ⚡ Транзакции (1 тр на кош без USDT или биржу, 8.55 TRX)

            👇 Пожалуйста, выберите кошелек, для которого вы желаете перевести энергию 👇
            """;

    private static final String MSG_NOT_ENOUGH_TRX = """
            ❌ Недостаточно средств на балансе
            Пожалуйста, пополните баланс и повторите попытку.
            """;

    private static final String MSG_TRANSACTION_PROGRESS = "Работаем, пожалуйста, подождите...";

    public static final String MSG_TRANSACTION_SUCCESS = """
            ✅ Транзакция успешно завершена
            Энергия была переведена на ваш кошелек
            """;

    private static final String MSG_TRANSACTION_PENDING = """
            ⏳ Транзакция в процессе
            Пожалуйста, подождите до 5 минут. Если транзакция не завершится, средства будут возвращены на ваш баланс.
            Бот отправит вам уведомление, когда транзакция завершится.
            """;

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void updMenuToPromptBalanceType(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_TRANSACTION_PROMPT_BALANCE_TYPE)
                .replyMarkup(getChooseBalanceTypeMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransaction65kMenu(List<UserWallet> wallets, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_TRANSACTION_65K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
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
                .text(MSG_TRANSACTION_131K_TEXT)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void notEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_NOT_ENOUGH_TRX)
                .replyMarkup(commonViews.getToMainMenuMarkup())
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
                .text(MSG_TRANSACTION_PROGRESS)
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
                .text(MSG_TRANSACTION_SUCCESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
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
                .text(MSG_TRANSACTION_PENDING)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
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
                                        .build())

                )
                .build();
    }

    public InlineKeyboardMarkup getChooseBalanceTypeMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(LBL_TRANSACTION_BALANCE_PERSONAL)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_BALANCE_PERSONAL)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(LBL_TRANSACTION_BALANCE_GROUP)
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_BALANCE_GROUP)
                                        .build()))
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
}
