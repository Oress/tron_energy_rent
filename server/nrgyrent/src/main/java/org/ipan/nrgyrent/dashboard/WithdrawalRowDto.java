package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "Выводы Пользователей" report table.
 *
 * Column mapping (SQL -> field):
 * id -> id, Дата -> date, Ник -> nickname, Имя -> name, Тип Баланса -> balanceType,
 * Сумма -> amount, Комиссия -> commission, Статус -> status, Получатель -> recipient,
 * Транзакция -> transaction.
 */
public record WithdrawalRowDto(
        Long id,
        LocalDateTime date,
        String nickname,
        String name,
        String balanceType,
        BigDecimal amount,
        BigDecimal commission,
        String status,
        String recipient,
        String transaction) {
}
