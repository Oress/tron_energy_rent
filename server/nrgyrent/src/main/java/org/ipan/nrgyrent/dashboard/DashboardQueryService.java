package org.ipan.nrgyrent.dashboard;

import java.math.BigDecimal;
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

    /**
     * SELECT part of the "Прибыль по пользователям" report — adapted from the Metabase
     * query. Aggregates per balance: completed orders + referral commissions + manual
     * balance changes (the latter two filtered by the period via CTEs).
     */
    private static final String USER_PROFIT_SELECT = """
            select
                ((b.created_at at time zone 'UTC') at time zone 'Turkey')        as "joined_at",
                ref_u.telegram_username                                           as "referral",
                case when b.type = 'GROUP' then 'Груповой' else 'Личный' end     as "balance_type",
                coalesce(b.label, nrg_users.telegram_username)                    as "group_login",
                nrg_users.telegram_first_name                                     as "name",
                coalesce(sum(o.sun_amount) - sum(o.itrx_fee_sun_amount)
                    - coalesce(sum(c.amount_sun), 0)
                    + coalesce((select sum(mc.amount_from - mc.amount_to)
                                from filtered_changes mc where mc.balance_id = b.id), 0), 0) as "profit",
                b.sun_balance                                                     as "available_balance",
                sum(o.sun_amount)                                                 as "income",
                sum(o.itrx_fee_sun_amount)                                        as "itrx_commission",
                coalesce(sum(c.amount_sun), 0)                                    as "referral_deductions",
                coalesce((select sum(mc.amount_from - mc.amount_to)
                          from filtered_changes mc where mc.balance_id = b.id), 0) as "manual_adjustments",
                t.label                                                           as "tariff"
            from nrg_balances b
                left join nrg_users on b.id = nrg_users.balance_id
                left join nrg_balance_referral_programs brp on brp.id = b.bal_ref_prog_id
                left join nrg_balances ref_b on brp.balance_id = ref_b.id
                left join nrg_users ref_u on ref_u.balance_id = ref_b.id
                left join filtered_orders o on o.balance_id = b.id
                left join nrg_referral_commissions c on o.id = c.order_id
                left join nrg_tariffs t on b.tariff_id = t.id
            """;

    private static final String USER_PROFIT_GROUP_BY = """
            group by b.id,
                b.type,
                coalesce(b.label, nrg_users.telegram_username),
                nrg_users.telegram_first_name,
                b.sun_balance,
                b.created_at,
                t.label,
                ref_u.telegram_username,
                coalesce((select sum(mc.amount_from - mc.amount_to)
                          from filtered_changes mc where mc.balance_id = b.id), 0)
            """;

    private static final String USER_PROFIT_ORDER_BY = """
            order by coalesce(sum(o.sun_amount) - sum(o.itrx_fee_sun_amount)
                - coalesce(sum(c.amount_sun), 0)
                + coalesce((select sum(mc.amount_from - mc.amount_to)
                            from filtered_changes mc where mc.balance_id = b.id), 0), 0) desc
            """;

    private static final RowMapper<UserProfitRowDto> USER_PROFIT_ROW_MAPPER = (rs, rowNum) -> new UserProfitRowDto(
            rs.getObject("joined_at", LocalDateTime.class),
            rs.getString("referral"),
            rs.getString("balance_type"),
            rs.getString("group_login"),
            rs.getString("name"),
            rs.getBigDecimal("profit"),
            rs.getString("tariff"),
            rs.getBigDecimal("available_balance"),
            rs.getBigDecimal("income"),
            rs.getBigDecimal("itrx_commission"),
            rs.getBigDecimal("referral_deductions"),
            rs.getBigDecimal("manual_adjustments"));

    /**
     * SELECT part of the "Депозиты" report — adapted from the Metabase query.
     * nrg_deposit_transactions.timestamp stores epoch milliseconds (UTC).
     */
    private static final String DEPOSITS_SELECT = """
            select
                d.id                                                          as "id",
                ((to_timestamp(d.timestamp::bigint / 1000)) at time zone 'Turkey') as "date",
                d.amount                                                      as "trx",
                d.original_amount                                             as "usdt",
                case when b.type = 'GROUP' then 'Груповой' else 'Личный' end as "balance_type",
                coalesce(b.label, nrg_users.telegram_username)                as "group_login",
                nrg_users.telegram_first_name                                 as "name",
                (coalesce(d.activation_fee_sun, 0) > 0)                       as "account_activated",
                d.wallet_to                                                   as "deposit_wallet",
                d.wallet_from                                                 as "sender",
                d.status                                                      as "status",
                d.tx_id                                                       as "transaction"
            from nrg_deposit_transactions d
                join nrg_balances b on d.wallet_to = b.deposit_address
                left join nrg_users on b.id = nrg_users.balance_id
            """;

    private static final String DEPOSITS_FROM_JOINS = """
            from nrg_deposit_transactions d
                join nrg_balances b on d.wallet_to = b.deposit_address
                left join nrg_users on b.id = nrg_users.balance_id
            """;

    private static final RowMapper<DepositRowDto> DEPOSITS_ROW_MAPPER = (rs, rowNum) -> new DepositRowDto(
            rs.getLong("id"),
            rs.getObject("date", LocalDateTime.class),
            rs.getBigDecimal("trx"),
            rs.getBigDecimal("usdt"),
            rs.getString("balance_type"),
            rs.getString("group_login"),
            rs.getString("name"),
            rs.getBoolean("account_activated"),
            rs.getString("deposit_wallet"),
            rs.getString("sender"),
            rs.getString("status"),
            rs.getString("transaction"));

    /**
     * Блок "Общая статистика" — adapted from the Metabase query. One row of metrics;
     * period-limited subqueries use the global filter's dateFrom/dateTo (inclusive,
     * Turkey time). "Новое Поле" is computed as in Metabase:
     * sweep + bybit*1e6 + catfee + itrx + trxx + netts − user balances − pending payouts.
     */
    private static final String STATISTICS_SQL = """
            with filtered_changes as (
                select * from nrg_manual_balance_changes c where 1 = 1%s
            )
            select t.*,
                (t."sweep_wallets_balance" + t."bybit_balance" * 1000000
                 + t."catfee_balance" + t."itrx_balance" + t."trxx_balance" + t."netts_balance"
                 - t."users_balance_sum" - coalesce(t."pending_referral_payouts", 0)) as "new_field"
            from (
                select
                    (select coalesce(sum(o.paid_sun - o.fee_sun), 0)
                     from nrg_aml_verifications o
                     where o.payment_status = 'COMPLETED'%s) as "aml_profit_period",
                    (select coalesce(sum(o.sun_amount - o.itrx_fee_sun_amount), 0)
                        - coalesce(sum(c.amount_sun), 0)
                        + coalesce((select sum(mc.amount_from - mc.amount_to) from filtered_changes mc), 0)
                     from nrg_orders o
                        left join nrg_referral_commissions c on o.id = c.order_id
                     where o.order_status = 'COMPLETED'%s) as "profit_period",
                    (select sum(o.itrx_fee_sun_amount)
                     from nrg_orders o
                     where o.order_status = 'COMPLETED'%s) as "providers_commission_period",
                    (select sum(d.amount_sun)
                     from nrg_referral_commission_deposits d
                     where 1 = 1%s) as "referral_payouts_period",
                    (select sum(o.ref_program_profit_remainder)
                     from nrg_orders o
                     where o.order_status = 'COMPLETED'%s) as "referral_programs_remainder",
                    (select sum(b.sun_balance) from nrg_balances b where b.is_active = true) as "users_balance_sum",
                    (select sum(d.amount) from nrg_deposit_transactions d) as "deposits_sum",
                    (select sum(w.balance_on_chain) from nrg_collection_wallets w) as "sweep_wallets_balance",
                    (select bbit.balance from nrg_bybit_balance bbit where bbit.coin = 'TRX') as "bybit_balance",
                    (select ib.balance from nrg_itrx_balance ib where ib.id = 'ITRX') as "itrx_balance",
                    (select sum(rc.amount_sun) from nrg_referral_commissions rc where rc.status = 'PENDING') as "pending_referral_payouts",
                    (select ib.balance from nrg_itrx_balance ib where ib.id = 'CATFEE') as "catfee_balance",
                    (select ib.balance from nrg_itrx_balance ib where ib.id = 'TRXX') as "trxx_balance",
                    (select ib.balance from nrg_itrx_balance ib where ib.id = 'NETTS.IO') as "netts_balance"
            ) t
            """;

    private static final RowMapper<OverallStatisticsDto> STATISTICS_ROW_MAPPER = (rs, rowNum) -> new OverallStatisticsDto(
            rs.getBigDecimal("profit_period"),
            rs.getBigDecimal("bybit_balance"),
            rs.getBigDecimal("aml_profit_period"),
            rs.getBigDecimal("pending_referral_payouts"),
            rs.getBigDecimal("itrx_balance"),
            rs.getBigDecimal("catfee_balance"),
            rs.getBigDecimal("trxx_balance"),
            rs.getBigDecimal("netts_balance"),
            rs.getBigDecimal("sweep_wallets_balance"),
            rs.getBigDecimal("users_balance_sum"),
            rs.getBigDecimal("referral_payouts_period"),
            rs.getBigDecimal("referral_programs_remainder"),
            rs.getBigDecimal("deposits_sum"),
            rs.getBigDecimal("providers_commission_period"),
            toPlainString(rs.getBigDecimal("new_field")));

    /**
     * Recursive CTE of the "Реферальная система" report — adapted from the Metabase query.
     * Anchor rows: referrers with their program, pending commission and paid amounts
     * (%s = period filter on the paid-amount subquery). Recursive rows: referred balances
     * under each referrer (deduplicated by the UNION).
     */
    private static final String REFERRAL_SYSTEM_CTE = """
            with recursive referrals as (
                select
                    pu.telegram_id as p_id,
                    null::bigint as u_id,
                    null::bigint as ub_id,
                    coalesce(pu.telegram_username, '<NO_LOGIN>') || '/' || pu.telegram_first_name as p_referrer,
                    null as p_referral,
                    rp.label as p_program,
                    (select sum(rc.amount_sun)
                     from nrg_referral_commissions rc
                     where rc.bal_ref_prog_id = pbrp.id
                       and rc.status = 'PENDING') as p_pending_commission,
                    (select sum(d.amount_sun)
                     from nrg_referral_commission_deposits d
                     where d.to_balance_id = pbrp.balance_id%s) as p_paid,
                    (select sum(d.amount_sun)
                     from nrg_referral_commission_deposits d
                     where d.to_balance_id = pbrp.balance_id) as p_paid_all
                from nrg_users pu
                    join nrg_balances pb on pb.id = pu.balance_id
                    join nrg_balance_referral_programs pbrp on pbrp.balance_id = pb.id
                    join nrg_referral_programs rp on rp.id = pbrp.ref_prog_id
                union
                select
                    pu.telegram_id as p_id,
                    u.telegram_id as u_id,
                    ub.id as ub_id,
                    coalesce(pu.telegram_username, '<NO_LOGIN>') || '/' || pu.telegram_first_name as p_referrer,
                    coalesce(ub.label, coalesce(u.telegram_username, '<NO_LOGIN>') || '/' || u.telegram_first_name) as p_referral,
                    null as p_program,
                    null as p_pending_commission,
                    null as p_paid,
                    null as p_paid_all
                from referrals
                    join nrg_users pu on pu.telegram_id = referrals.p_id
                    join nrg_balances pb on pb.id = pu.balance_id
                    join nrg_balance_referral_programs pbrp on pbrp.balance_id = pb.id
                    join nrg_balances ub on ub.bal_ref_prog_id = pbrp.id
                    left join nrg_users u on u.balance_id = ub.id
            )
            """;

    private static final RowMapper<ReferralSystemRowDto> REFERRAL_SYSTEM_ROW_MAPPER = (rs, rowNum) -> new ReferralSystemRowDto(
            rs.getString("referrer"),
            rs.getString("referral"),
            rs.getString("program"),
            rs.getBigDecimal("pending_payout"),
            rs.getBigDecimal("paid_period"),
            rs.getBigDecimal("paid_total"));

    private static Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private static String toPlainString(BigDecimal value) {
        return value == null ? null : value.toPlainString();
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
     * "Прибыль по пользователям" report (adapted from the Metabase query).
     * Global filter:
     * - userId: row belongs to the user owning the balance ({@code nrg_users.telegram_id});
     * - groupId: id of a GROUP-type balance — matches the group balance itself and the
     *   individual balances of users attached to it ({@code nrg_users.group_balance_id});
     * - dateFrom/dateTo: inclusive, applied to completed orders and manual balance changes
     *   (in Turkey time). Balances are always listed; only the aggregates are period-limited.
     */
    public PageDto<UserProfitRowDto> getUserProfitPage(int page, int size,
                                                       Long userId, Long groupId,
                                                       LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuilder changesFilter = new StringBuilder();
        StringBuilder ordersFilter = new StringBuilder();
        if (dateFrom != null) {
            changesFilter.append(" and ((c.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            ordersFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            params.addValue("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            changesFilter.append(" and ((c.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            ordersFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            params.addValue("dateTo", dateTo);
        }

        StringBuilder where = new StringBuilder(" where 1 = 1");
        if (userId != null) {
            where.append(" and nrg_users.telegram_id = :userId");
            params.addValue("userId", userId);
        }
        if (groupId != null) {
            where.append(" and (b.id = :groupId or nrg_users.group_balance_id = :groupId)");
            params.addValue("groupId", groupId);
        }

        String ctes = "with filtered_changes as ("
                + " select * from nrg_manual_balance_changes c where 1 = 1" + changesFilter + "),"
                + " filtered_orders as ("
                + " select * from nrg_orders o where o.order_status = 'COMPLETED'" + ordersFilter + ")";

        Long totalElements = jdbc.queryForObject(
                "select count(*) from nrg_balances b"
                        + " left join nrg_users on b.id = nrg_users.balance_id" + where,
                params, Long.class);

        params.addValue("limit", boundedSize);
        params.addValue("offset", (long) Math.max(page, 0) * boundedSize);
        List<UserProfitRowDto> rows = jdbc.query(
                ctes + USER_PROFIT_SELECT + where + USER_PROFIT_GROUP_BY
                        + USER_PROFIT_ORDER_BY + " limit :limit offset :offset",
                params, USER_PROFIT_ROW_MAPPER);

        return new PageDto<>(rows, page, boundedSize, totalElements == null ? 0 : totalElements);
    }

    /**
     * "Депозиты" report (adapted from the Metabase query).
     * Global filter:
     * - userId: deposit belongs to the user owning the balance ({@code nrg_users.telegram_id});
     * - groupId: id of a GROUP-type balance — matches deposits on the group balance itself
     *   and on balances of users attached to it ({@code nrg_users.group_balance_id});
     * - dateFrom/dateTo: inclusive, applied to the deposit date in Turkey time.
     */
    public PageDto<DepositRowDto> getDepositsPage(int page, int size,
                                                  Long userId, Long groupId,
                                                  LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuilder where = new StringBuilder(" where 1 = 1");
        if (userId != null) {
            where.append(" and nrg_users.telegram_id = :userId");
            params.addValue("userId", userId);
        }
        if (groupId != null) {
            where.append(" and (b.id = :groupId or nrg_users.group_balance_id = :groupId)");
            params.addValue("groupId", groupId);
        }
        if (dateFrom != null) {
            where.append(" and ((to_timestamp(d.timestamp::bigint / 1000)) at time zone 'Turkey')::date >= :dateFrom");
            params.addValue("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            where.append(" and ((to_timestamp(d.timestamp::bigint / 1000)) at time zone 'Turkey')::date <= :dateTo");
            params.addValue("dateTo", dateTo);
        }

        Long totalElements = jdbc.queryForObject(
                "select count(*) " + DEPOSITS_FROM_JOINS + where, params, Long.class);

        params.addValue("limit", boundedSize);
        params.addValue("offset", (long) Math.max(page, 0) * boundedSize);
        List<DepositRowDto> rows = jdbc.query(
                DEPOSITS_SELECT + where + " order by d.id desc limit :limit offset :offset",
                params, DEPOSITS_ROW_MAPPER);

        return new PageDto<>(rows, page, boundedSize, totalElements == null ? 0 : totalElements);
    }

    /**
     * "Реферальная система" report (adapted from the Metabase recursive-CTE query).
     * Global filter:
     * - userId: rows of the given referrer ({@code p_id});
     * - groupId: referred balances that are the group balance itself ({@code ub_id}), or
     *   rows of referrers attached to the group ({@code nrg_users.group_balance_id});
     * - dateFrom/dateTo: inclusive, applied to "Выплачено за период" (commission deposits
     *   in Turkey time); "Выплачено всего" is not period-limited (as in Metabase).
     */
    public PageDto<ReferralSystemRowDto> getReferralSystemPage(int page, int size,
                                                               Long userId, Long groupId,
                                                               LocalDate dateFrom, LocalDate dateTo) {
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuilder paidPeriodFilter = new StringBuilder();
        if (dateFrom != null) {
            paidPeriodFilter.append(" and ((d.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            params.addValue("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            paidPeriodFilter.append(" and ((d.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            params.addValue("dateTo", dateTo);
        }

        StringBuilder where = new StringBuilder(" where 1 = 1");
        if (userId != null) {
            where.append(" and p_id = :userId");
            params.addValue("userId", userId);
        }
        if (groupId != null) {
            where.append(" and (ub_id = :groupId or p_id in (select telegram_id from nrg_users where group_balance_id = :groupId))");
            params.addValue("groupId", groupId);
        }

        String cte = REFERRAL_SYSTEM_CTE.formatted(paidPeriodFilter);

        Long totalElements = jdbc.queryForObject(
                cte + "select count(*) from referrals" + where, params, Long.class);

        params.addValue("limit", boundedSize);
        params.addValue("offset", (long) Math.max(page, 0) * boundedSize);
        List<ReferralSystemRowDto> rows = jdbc.query(
                cte + """
                        select
                            p_referrer            as "referrer",
                            p_referral            as "referral",
                            p_program             as "program",
                            p_pending_commission  as "pending_payout",
                            p_paid                as "paid_period",
                            p_paid_all            as "paid_total"
                        from referrals
                        """ + where
                        + " order by p_referrer, p_id desc, ub_id desc limit :limit offset :offset",
                params, REFERRAL_SYSTEM_ROW_MAPPER);

        return new PageDto<>(rows, page, boundedSize, totalElements == null ? 0 : totalElements);
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
     * Блок "Общая статистика" (adapted from the Metabase query).
     * dateFrom/dateTo (inclusive) limit the "(за период)" metrics in Turkey time;
     * the non-period metrics (balances, deposits sum, pending payouts) are not filtered.
     */
    public OverallStatisticsDto getOverallStatistics(LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder changesFilter = new StringBuilder();
        StringBuilder amlFilter = new StringBuilder();
        StringBuilder ordersFilter = new StringBuilder();
        StringBuilder refDepositsFilter = new StringBuilder();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (dateFrom != null) {
            changesFilter.append(" and ((c.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            amlFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            ordersFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            refDepositsFilter.append(" and ((d.created_at at time zone 'UTC') at time zone 'Turkey')::date >= :dateFrom");
            params.addValue("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            changesFilter.append(" and ((c.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            amlFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            ordersFilter.append(" and ((o.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            refDepositsFilter.append(" and ((d.created_at at time zone 'UTC') at time zone 'Turkey')::date <= :dateTo");
            params.addValue("dateTo", dateTo);
        }

        String sql = STATISTICS_SQL.formatted(changesFilter, amlFilter, ordersFilter,
                ordersFilter, refDepositsFilter, ordersFilter);
        return jdbc.queryForObject(sql, params, STATISTICS_ROW_MAPPER);
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
