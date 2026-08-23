package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "Выплаты по реф программам" report table.
 *
 * Column mapping (SQL -> field):
 * Пользователь -> user, Сумма -> amount, Дата -> date.
 */
public record ReferralPayoutRowDto(
        String user,
        BigDecimal amount,
        LocalDateTime date) {
}
