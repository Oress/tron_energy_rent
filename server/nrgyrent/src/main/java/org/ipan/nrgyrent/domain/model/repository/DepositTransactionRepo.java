package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositTransactionRepo extends JpaRepository<DepositTransaction, Long> {
    DepositTransaction findBySystemOrder(Order systemOrder);

    DepositTransaction findBySystemOrderCorrelationId(String systemOrderCorrelationId);

    DepositTransaction findByTxId(String txId);


    @Query(nativeQuery = true, value = """
    select d.tx_id, d.amount, d.original_amount, d.timestamp from nrg_deposit_transactions d
    join nrg_balances b on b.deposit_address = d.wallet_to
    where b.id = :balanceId
    order by d.id desc
    """,
    countQuery = """
        select d.tx_id, d.amount, d.original_amount, d.timestamp from nrg_deposit_transactions d
        join nrg_balances b on b.deposit_address = d.wallet_to
        where b.id = :balanceId
    """)
    Page<DepositHistoryItem> findAllByByBalanceId(Long balanceId, Pageable of);
}

