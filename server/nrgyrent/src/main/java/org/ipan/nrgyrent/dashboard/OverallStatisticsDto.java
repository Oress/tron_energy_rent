package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;

/**
 * Блок "Общая статистика" — label/value metrics.
 *
 * Metric mapping:
 * Прибыль (за период) -> profitPeriod, Баланс Bybit -> bybitBalance,
 * AML прибыль (за период) -> amlProfitPeriod,
 * Реф. выплаты в ожидании -> pendingReferralPayouts,
 * Баланс itrx.io -> itrxBalance, Баланс catfee.io -> catfeeBalance,
 * Баланс trxx.io -> trxxBalance, Баланс netts.io -> nettsBalance,
 * Баланс свип кошельков -> sweepWalletsBalance,
 * Сумма балансов юзеров -> usersBalanceSum,
 * Реф. выплаты (за период) -> referralPayoutsPeriod,
 * Остаток от реф. программ (за период) -> referralProgramsRemainder,
 * Сумма депозитов -> depositsSum,
 * Комиссия поставщиков (период) -> providersCommissionPeriod,
 * Новое Поле (резерв) -> newField.
 */
public record OverallStatisticsDto(
        BigDecimal profitPeriod,
        BigDecimal bybitBalance,
        BigDecimal amlProfitPeriod,
        BigDecimal pendingReferralPayouts,
        BigDecimal itrxBalance,
        BigDecimal catfeeBalance,
        BigDecimal trxxBalance,
        BigDecimal nettsBalance,
        BigDecimal sweepWalletsBalance,
        BigDecimal usersBalanceSum,
        BigDecimal referralPayoutsPeriod,
        BigDecimal referralProgramsRemainder,
        BigDecimal depositsSum,
        BigDecimal providersCommissionPeriod,
        String newField) {

    public static OverallStatisticsDto empty() {
        return new OverallStatisticsDto(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
