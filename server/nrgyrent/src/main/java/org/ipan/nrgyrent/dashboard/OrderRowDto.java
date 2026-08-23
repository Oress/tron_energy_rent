package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "Заказы" report table.
 *
 * Column mapping (SQL -> field):
 * ID -> id, Дата -> date, Логин -> login, Имя -> name, Тип Баланса -> balanceType,
 * Кол. транзакций -> transactionsCount, Энергия всего -> totalEnergy, Тариф -> tariff,
 * Провайдер -> provider, Комиссия -> commission, Доход -> income (nrg_orders.sun_amount),
 * Реф. отчисления -> referralDeductions, Остаток реф. отчисл. -> referralDeductionsRemainder,
 * Прибыль -> profit, Активация кош. -> walletActivated, Статус -> status,
 * Получатель -> recipient, № заказа -> orderNumber, Транзакция -> transaction.
 */
public record OrderRowDto(
        Long id,
        LocalDateTime date,
        String login,
        String name,
        String balanceType,
        Integer transactionsCount,
        Long totalEnergy,
        String tariff,
        String provider,
        BigDecimal commission,
        BigDecimal income,
        BigDecimal referralDeductions,
        BigDecimal referralDeductionsRemainder,
        BigDecimal profit,
        Boolean walletActivated,
        String status,
        String recipient,
        String orderNumber,
        String transaction) {
}
