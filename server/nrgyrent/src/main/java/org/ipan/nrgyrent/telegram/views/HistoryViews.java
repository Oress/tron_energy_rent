package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class HistoryViews {
    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void updMenuToHistoryMenu(List<Order> orders, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getHistoryMessage(orders))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    public String getHistoryMessage(List<Order> orders) {
        String history = orders.stream()
                .map(ord -> getTransactionDetails(ord))
                .collect(Collectors.joining("\n\n"));

        return """
                📜 История последних транзакций

                %s
                """.formatted(history);
    }

    // TODO: receiveAddress use alias if possible.
    private String getTransactionDetails(Order order) {
        return """
                ID: %s
                Сумма: %s TRX
                Получатель: %s
                Статус: %s
                Дата: %s
                """.formatted(
                order.getCorrelationId(),
                FormattingTools.formatBalance(order.getSunAmount()),
                WalletTools.formatTronAddress(order.getReceiveAddress()),
                FormattingTools.orderStatusLabel(order.getOrderStatus()),
                FormattingTools.formatDateToUtc(order.getCreatedAt()));
    }
}
