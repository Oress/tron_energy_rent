package org.ipan.nrgyrent.dashboard;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Data access for the dashboard report tables.
 * <p>
 * All queries are plain SQL with parameters (not JPA). Each method receives
 * {@code page} (0-based) and {@code size} and must return a {@link PageDto} with
 * {@code totalElements} filled from a matching COUNT query.
 */
@Service
public class DashboardQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    /**
     * "Заказы" report.
     * Expected result columns (in order): id, date, login, name, balance_type,
     * transactions_count, total_energy, tariff, provider, commission, income,
     * referral_deductions, referral_deductions_remainder, profit, wallet_activated,
     * status, recipient, order_number, transaction.
     */
    public PageDto<OrderRowDto> getOrdersPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<OrderRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<OrderRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Прибыль по пользователям" report.
     * Expected result columns (in order): joined_at, referral, balance_type,
     * group_login, name, profit, tariff, available_balance, income, itrx_commission,
     * referral_deductions, manual_adjustments.
     */
    public PageDto<UserProfitRowDto> getUserProfitPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<UserProfitRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<UserProfitRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Депозиты" report.
     * Expected result columns (in order): id, date, trx, usdt, balance_type,
     * group_login, name, account_activated, deposit_wallet, sender, status, transaction.
     */
    public PageDto<DepositRowDto> getDepositsPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<DepositRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<DepositRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * Блок "Общая статистика" — single row of metrics.
     * Expected result columns (in order): profit_period, bybit_balance, aml_profit_period,
     * pending_referral_payouts, itrx_balance, catfee_balance, trxx_balance, netts_balance,
     * sweep_wallets_balance, users_balance_sum, referral_payouts_period,
     * referral_programs_remainder, deposits_sum, providers_commission_period, new_field.
     */
    public OverallStatisticsDto getOverallStatistics() {
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        // For now returns an empty DTO so the endpoint/FE contract can be verified.
        return OverallStatisticsDto.empty();
    }

    /**
     * "Реферальная система" report.
     * Expected result columns (in order): referrer, referral, program, pending_payout,
     * paid_period, paid_total.
     */
    public PageDto<ReferralSystemRowDto> getReferralSystemPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<ReferralSystemRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<ReferralSystemRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Выплаты по реф программам" report.
     * Expected result columns (in order): user, amount, date.
     */
    public PageDto<ReferralPayoutRowDto> getReferralPayoutsPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<ReferralPayoutRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<ReferralPayoutRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Выводы Пользователей" report.
     * Expected result columns (in order): id, date, nickname, name, balance_type,
     * amount, commission, status, recipient, transaction.
     */
    public PageDto<WithdrawalRowDto> getWithdrawalsPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<WithdrawalRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<WithdrawalRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "AML проверки" report.
     * Expected result columns (in order): id, date, nickname, name, balance_type,
     * provider, tariff, check_wallet, amount, profit, commission, status,
     * internal_status, order_number.
     */
    public PageDto<AmlCheckRowDto> getAmlChecksPage(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate):
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<AmlCheckRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        // For now returns an empty page so the endpoint/FE contract can be verified.
        List<AmlCheckRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }
}
