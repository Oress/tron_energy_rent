package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "AML проверки" report table.
 *
 * Column mapping (SQL -> field):
 * Id -> id, Дата -> date, Ник -> nickname, Имя -> name, Тип Баланса -> balanceType,
 * Провайдер -> provider, Тариф -> tariff, Кош. для проверки -> checkWallet,
 * Сумма -> amount, Прибыль -> profit, Комиссия -> commission, Статус -> status,
 * Статус Внутренний -> internalStatus, № заказа -> orderNumber.
 */
public record AmlCheckRowDto(
        Long id,
        LocalDateTime date,
        String nickname,
        String name,
        String balanceType,
        String provider,
        String tariff,
        String checkWallet,
        BigDecimal amount,
        BigDecimal profit,
        BigDecimal commission,
        String status,
        String internalStatus,
        String orderNumber) {
}
