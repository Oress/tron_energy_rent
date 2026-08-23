package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of the "Прибыль по пользователям" report table.
 *
 * Column mapping (SQL -> field):
 * Присоединились -> joinedAt, Реферал -> referral, Тип Баланса -> balanceType,
 * Группа\Логин -> groupLogin, Имя -> name, Прибыль -> profit, Тариф -> tariff,
 * Доступно на балансе -> availableBalance, Доход -> income,
 * Комиссия ITRX -> itrxCommission, Реф. отчисления -> referralDeductions,
 * Ручные изменения -> manualAdjustments.
 */
public record UserProfitRowDto(
        LocalDateTime joinedAt,
        String referral,
        String balanceType,
        String groupLogin,
        String name,
        BigDecimal profit,
        String tariff,
        BigDecimal availableBalance,
        BigDecimal income,
        BigDecimal itrxCommission,
        BigDecimal referralDeductions,
        BigDecimal manualAdjustments) {
}
