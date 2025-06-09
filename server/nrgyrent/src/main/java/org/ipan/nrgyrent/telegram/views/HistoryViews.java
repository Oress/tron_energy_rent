package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.model.projections.TransactionHistoryDto;
import org.ipan.nrgyrent.telegram.i18n.HistoryLabels;
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
    private final HistoryLabels historyLabels;
    private final FormattingTools formattingTools;

    @Retryable
    @SneakyThrows
    public void updMenuToHistoryMenu(List<TransactionHistoryDto> orders, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getHistoryMessage(orders))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public String getHistoryMessage(List<TransactionHistoryDto> orders) {
        String history = orders.stream()
                .map(ord -> getTransactionDetails(ord))
                .collect(Collectors.joining("\n\n"));

        return historyLabels.historyMsg(history);
    }

    private String getTransactionDetails(TransactionHistoryDto order) {
        String user = order.getUserId() != null ? formattingTools.formatUserForSearch(order.getUserId(),order.getUsername(), order.getFirstname()) : "";
        switch (order.getType()) {
            case "ORDER" -> {
                return historyLabels.itemTx(
                    order.getCorrelationId(),
                    user.isEmpty() ? "": historyLabels.itemTxMember(user),
                    order.getTxAmount(),
                    FormattingTools.formatBalance(order.getTotalAmountSun()),
                    WalletTools.formatTronAddress(order.getReceiveAddress()),
                    formattingTools.orderStatusLabel(OrderStatus.valueOf(order.getOrderStatus())),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? historyLabels.balanceGroup() : historyLabels.balancePersonal(),
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            case "WITHDRAWAL" -> {
                return historyLabels.itemWithdraw(
                    FormattingTools.formatBalance(order.getTotalAmountSun()),
                    WalletTools.formatTronAddress(order.getReceiveAddress()),
                    formattingTools.withdrawalStatusLabel(WithdrawalStatus.valueOf(order.getWithdrawalStatus())),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? historyLabels.balanceGroup() : historyLabels.balancePersonal(),
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            case "DEPOSIT" -> {
                return historyLabels.itemDeposit(
                    FormattingTools.formatBalance(order.getTotalAmountSun()),
                    WalletTools.formatTronAddress(order.getFromAddress()),
                    BalanceType.GROUP.name().equals(order.getBalanceType()) ? historyLabels.balanceGroup() : historyLabels.balancePersonal(),
                    FormattingTools.formatDateToUtc(order.getCreatedAt()));
            }
            default -> {
                return "Неизвестный тип транзакции: " + order.getType();
            }
        }
    }
}
