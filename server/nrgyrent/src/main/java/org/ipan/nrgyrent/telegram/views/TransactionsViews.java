package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class TransactionsViews {
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
            Пожалуйста, подождите 5 минут. Если транзакция не завершится, средства будут возвращены на ваш баланс.
            Бот отправит вам уведомление, когда транзакция будет завершена.
            """;

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

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
}
