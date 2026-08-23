package org.ipan.nrgyrent.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Data access for the dashboard report tables.
 * <p>
 * All queries are plain SQL with `parameters (not JPA). Each page method receives
 * {@code page} (0-based), {@code size} and the global dashboard filter
 * ({@code userId}, {@code groupId}, {@code dateFrom}, {@code dateTo} — all nullable)
 * and must return a {@link PageDto} with {@code totalElements} filled from a matching
 * COUNT query.
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * SELECT part of the "Заказы" report — adapted from the Metabase query.
     * Dates are stored as UTC timestamps and converted to Turkey time for display.
     */
    private static final String ORDERS_SELECT = """
            select
                nrg_orders.id                                                     as "id",
                ((nrg_orders.created_at at time zone 'UTC') at time zone 'Turkey') as "date",
                coalesce(nrg_users.telegram_username, '-')                        as "login",
                nrg_users.telegram_first_name                                     as "name",
                case when b.type = 'GROUP' then 'Груповой' else 'Личный' end      as "balance_type",
                nrg_orders.tx_amount                                              as "transactions_count",
                nrg_orders.energy_amount                                          as "total_energy",
                t.label                                                           as "tariff",
                nrg_orders.energy_provider                                        as "provider",
                nrg_orders.itrx_fee_sun_amount                                    as "commission",
                nrg_orders.sun_amount                                             as "income",
                coalesce(c.amount_sun, 0)                                         as "referral_deductions",
                coalesce(nrg_orders.ref_program_profit_remainder, 0)              as "referral_deductions_remainder",
                nrg_orders.sun_amount - coalesce(c.amount_sun, 0)
                    - nrg_orders.itrx_fee_sun_amount                              as "profit",
                (coalesce(nrg_orders.activation_fee, 0) > 0)                      as "wallet_activated",
                nrg_orders.order_status                                           as "status",
                nrg_orders.receive_address                                        as "recipient",
                nrg_orders.serial                                                 as "order_number",
                nrg_orders.tx_id                                                  as "transaction"
            from nrg_orders
                join nrg_users on nrg_orders.user_id = nrg_users.telegram_id
                join nrg_balances b on nrg_orders.balance_id = b.id
                join nrg_tariffs t on nrg_orders.tariff_id = t.id
                left join nrg_referral_commissions c on nrg_orders.id = c.order_id
            """;

    private static final String ORDERS_FROM_JOINS = """
            from nrg_orders
                join nrg_users on nrg_orders.user_id = nrg_users.telegram_id
                join nrg_balances b on nrg_orders.balance_id = b.id
                join nrg_tariffs t on nrg_orders.tariff_id = t.id
                left join nrg_referral_commissions c on nrg_orders.id = c.order_id
            """;

    private static final RowMapper<OrderRowDto> ORDERS_ROW_MAPPER = (rs, rowNum) -> new OrderRowDto(
            rs.getLong("id"),
            rs.getObject("date", LocalDateTime.class),
            rs.getString("login"),
            rs.getString("name"),
            rs.getString("balance_type"),
            rs.getObject("transactions_count", Integer.class),
            toLong(rs.getObject("total_energy", Integer.class)),
            rs.getString("tariff"),
            rs.getString("provider"),
            rs.getBigDecimal("commission"),
            rs.getBigDecimal("income"),
            rs.getBigDecimal("referral_deductions"),
            rs.getBigDecimal("referral_deductions_remainder"),
            rs.getBigDecimal("profit"),
            rs.getBoolean("wallet_activated"),
            rs.getString("status"),
            rs.getString("recipient"),
            rs.getString("order_number"),
            rs.getString("transaction"));

    private static Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    /**
     * "Заказы" report (adapted from the Metabase query).
     * Global filter:
     * - userId: order belongs to the user ({@code nrg_orders.user_id});
     * - groupId: id of a GROUP-type balance — matches orders placed on the group balance
     *   or by users attached to it ({@code nrg_users.group_balance_id});
     * - dateFrom/dateTo: inclusive, applied to the order date in Turkey time.
     */
    public PageDto<OrderRowDto> getOrdersPage(int page, int size,
                                              Long userId, Long groupId,
                                              LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        StringBuilder where = new StringBuilder(" where 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (userId != null) {
            where.append(" and nrg_orders.user_id = :userId");
            params.addValue("userId", userId);
        }
        if (groupId != null) {
            where.append(" and (nrg_orders.balance_id = :groupId or nrg_users.group_balance_id = :groupId)");
            params.addValue("groupId", groupId);
        }
        if (dateFrom != null) {
            where.append(" and ((nrg_orders.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            params.addValue("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            where.append(" and ((nrg_orders.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            params.addValue("dateTo", dateTo);
        }

        Long totalElements = jdbc.queryForObject(
                "select count(*) " + ORDERS_FROM_JOINS + where, params, Long.class);

        params.addValue("limit", boundedSize);
        params.addValue("offset", (long) Math.max(page, 0) * boundedSize);
        List<OrderRowDto> rows = jdbc.query(
                ORDERS_SELECT + where + " order by nrg_orders.id desc limit :limit offset :offset",
                params, ORDERS_ROW_MAPPER);

        return new PageDto<>(rows, page, boundedSize, totalElements == null ? 0 : totalElements);
    }

    /**
     * "Прибыль по пользователям" report.
     * Expected result columns (in order): joined_at, referral, balance_type,
     * group_login, name, profit, tariff, available_balance, income, itrx_commission,
     * referral_deductions, manual_adjustments.
     * Filter: user/group by id, period on the joined_at column.
     */
    public PageDto<UserProfitRowDto> getUserProfitPage(int page, int size,
                                                       Long userId, Long groupId,
                                                       LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<UserProfitRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<UserProfitRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Депозиты" report.
     * Expected result columns (in order): id, date, trx, usdt, balance_type,
     * group_login, name, account_activated, deposit_wallet, sender, status, transaction.
     * Filter: user/group by id, period on the date column.
     */
    public PageDto<DepositRowDto> getDepositsPage(int page, int size,
                                                  Long userId, Long groupId,
                                                  LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<DepositRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<DepositRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Реферальная система" report.
     * Expected result columns (in order): referrer, referral, program, pending_payout,
     * paid_period, paid_total.
     * Filter: user/group by id (referrer or referral side — depends on the query),
     * period on the relevant date column.
     */
    public PageDto<ReferralSystemRowDto> getReferralSystemPage(int page, int size,
                                                               Long userId, Long groupId,
                                                               LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<ReferralSystemRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<ReferralSystemRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Выплаты по реф программам" report.
     * Expected result columns (in order): user, amount, date.
     * Filter: user/group by id, period on the date column.
     */
    public PageDto<ReferralPayoutRowDto> getReferralPayoutsPage(int page, int size,
                                                                Long userId, Long groupId,
                                                                LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<ReferralPayoutRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<ReferralPayoutRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "Выводы Пользователей" report.
     * Expected result columns (in order): id, date, nickname, name, balance_type,
     * amount, commission, status, recipient, transaction.
     * Filter: user/group by id, period on the date column.
     */
    public PageDto<WithdrawalRowDto> getWithdrawalsPage(int page, int size,
                                                        Long userId, Long groupId,
                                                        LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<WithdrawalRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<WithdrawalRowDto> rows = List.of();
        return new PageDto<>(rows, page, boundedSize, rows.size());
    }

    /**
     * "AML проверки" report.
     * Expected result columns (in order): id, date, nickname, name, balance_type,
     * provider, tariff, check_wallet, amount, profit, commission, status,
     * internal_status, order_number.
     * Filter: user/group by id, period on the date column.
     */
    public PageDto<AmlCheckRowDto> getAmlChecksPage(int page, int size,
                                                    Long userId, Long groupId,
                                                    LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        //  Same global-filter WHERE clauses as getOrdersPage.
        //  1) SELECT ... LIMIT :size OFFSET :page*:size  -> List<AmlCheckRowDto>
        //  2) SELECT COUNT(*) -> totalElements
        List<AmlCheckRowDto> rows = List.of();
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
     * Options for the global filter's user selector (id + display label, e.g. login/name).
     */
    public List<FilterUserDto> getFilterUsers() {
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        // SELECT id, login || ' — ' || name AS label FROM ... ORDER BY label
        return List.of();
    }

    /**
     * Options for the global filter's group selector (id + display label).
     */
    public List<FilterGroupDto> getFilterGroups() {
        // TODO: implement queries (plain SQL with parameters, e.g. JdbcTemplate).
        // SELECT id, label FROM ... ORDER BY label
        return List.of();
    }
}
