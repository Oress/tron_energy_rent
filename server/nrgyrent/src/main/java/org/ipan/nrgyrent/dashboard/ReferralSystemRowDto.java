package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;

/**
 * Row of the "Реферальная система" report table.
 *
 * Column mapping (SQL -> field):
 * Реферер -> referrer, Реферал -> referral, Программа -> program,
 * Выплата в ожидании -> pendingPayout, Выплачено за период -> paidPeriod,
 * Выплачено всего -> paidTotal.
 */
public record ReferralSystemRowDto(
        String referrer,
        String referral,
        String program,
        BigDecimal pendingPayout,
        BigDecimal paidPeriod,
        BigDecimal paidTotal) {
}
