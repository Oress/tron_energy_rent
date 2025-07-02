package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepo extends JpaRepository<Balance, Long> {
    Page<Balance> findAllByTypeOrderById(BalanceType type, Pageable pageable);
    Page<Balance> findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType group, String label, PageRequest of);
    List<Balance> findAllByIsActive(Boolean isActive);
    List<Balance> findAllByTariffId(Long tariffId);

    @Query("select b.id from Balance b where b.isActive = :isActive")
    List<Long> findAllIdsByIsActive(Boolean isActive);

    @Query("select distinct b from ReferralCommission c join c.balanceReferralProgram brp join brp.balance b where c.status = ReferralCommissionStatus.PENDING")
    Set<Balance> findAllWithPendingReferralCommissions();

    Balance findByDepositAddress(String depositAddress);
}
