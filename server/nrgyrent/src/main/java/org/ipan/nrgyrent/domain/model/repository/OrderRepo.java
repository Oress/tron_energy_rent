package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;
import java.util.Optional;

import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.projections.TransactionHistoryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByCorrelationId(String correlationId);
    Page<Order> findAllByUserTelegramIdOrderByCreatedAtDesc(Long userId, PageRequest pageable);

    @Query(
    value = """
        SELECT * FROM (
        SELECT 
            'ORDER' AS type, ord.id, correlation_id as correlationId, order_status as orderStatus, receive_address as receiveAddress, null AS fromAddress, null AS withdrawalStatus, ord.tx_amount as txAmount, sun_amount AS totalAmountSun, ord.created_at,  b.type as balanceType 
            FROM nrg_orders ord join nrg_balances b on b.id = ord.balance_id
            where user_id = :userId 
        UNION
        SELECT 
            'WITHDRAWAL', wo.id, null as correlationId, null as orderStatus, receive_address as receiveAddress, null AS fromAddress, status as withdrawalStatus, null as txAmount, sun_amount AS totalAmountSun, wo.created_at, b.type as balanceType 
            FROM nrg_withdrawal_orders wo join nrg_balances b on b.id = wo.balance_id
            where user_id = :userId 
        UNION
        SELECT 'DEPOSIT', tr.id, null as correlationId, null as orderStatus, wallet_to as receiveAddress, wallet_from as fromAddress, null AS withdrawalStatus, null as txAmount, amount as totalAmountSun, to_timestamp(tr.timestamp::bigint/1000) AT TIME ZONE 'UTC' as createdAt, b.type as balanceType
            FROM
                nrg_deposit_transactions tr
                join nrg_balances b on b.deposit_address = tr.wallet_to
                left join nrg_users u on b.id = u.balance_id -- individual user balance
                left join nrg_users members on b.id = members.group_balance_id  -- individual user balance
            where
                (u.telegram_id = :userId or members.telegram_id = :userId)
        ) AS all_tx 
        ORDER BY created_at DESC
        LIMIT :limit
            """,
    nativeQuery = true)
    List<TransactionHistoryDto> findAllTransactions(Long userId,  int limit);
}
