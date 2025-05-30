package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.model.projections.TransactionHistoryDto;
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
    public void updMenuToHistoryMenu(List<TransactionHistoryDto> orders, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getHistoryMessage(orders))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    public String getHistoryMessage(List<TransactionHistoryDto> orders) {
        String history = orders.stream()
                .map(ord -> getTransactionDetails(ord))
                .collect(Collectors.joining("\n\n"));

        return """
                📜 История последних транзакций

                %s
                """.formatted(history);
    }

    private String getTransactionDetails(TransactionHistoryDto order) {
        switch (order.getType()) {
            case "ORDER" -> {
                    return """
                    Операция: Аренда транзакции
                    ID: %s
                    Сумма: %s TRX
                    Получатель: %s
                    Статус: %s
                    Баланс: %s
                    Дата: %s
                    """.formatted(
                    order.getCorrelationId(),
                    FormattingTools.formatBalance(order.getAmount()),
                    WalletTools.formatTronAddress(order.getReceiveAddress()),
                    FormattingTools.orderStatusLabel(OrderStatus.valueOf(order.getOrderStatus())),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? "Групповой" : "Личный",
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            case "WITHDRAWAL" -> {
                return """
                    Операция: Вывод TRX
                    Сумма: %s TRX
                    Получатель: %s
                    Статус: %s
                    Баланс: %s
                    Дата: %s
                    """.formatted(
                    FormattingTools.formatBalance(order.getAmount()),
                    WalletTools.formatTronAddress(order.getReceiveAddress()),
                    FormattingTools.withdrawalStatusLabel(WithdrawalStatus.valueOf(order.getWithdrawalStatus())),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? "Групповой" : "Личный",
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            case "DEPOSIT" -> {
                return """
                    Операция: Пополнение баланса
                    Сумма: %s TRX
                    Отправитель: %s
                    Баланс: %s
                    Дата: %s
                    """.formatted(
                    FormattingTools.formatBalance(order.getAmount()),
                    WalletTools.formatTronAddress(order.getFromAddress()),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? "Групповой" : "Личный",
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            default -> {
                return "Неизвестный тип транзакции: " + order.getType();
            }
        }
    }
}
