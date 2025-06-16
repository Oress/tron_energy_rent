package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;
import java.util.Optional;

import org.ipan.nrgyrent.domain.model.ReferralCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralCommissionRepo extends JpaRepository<ReferralCommission, Long> {

    @Query("select distinct c from ReferralCommission c join c.balanceReferralProgram brp join brp.balance b where c.status = ReferralCommissionStatus.PENDING and b.id = :balanceId")
    List<ReferralCommission> findAllPendingByBalanceId(Long balanceId);

    @Query("select sum(c.amountSun) from ReferralCommission c join c.balanceReferralProgram brp where c.status = ReferralCommissionStatus.PENDING and brp.id = :balRefProgram")
    Optional<Long> findSumOfAllPendingByBalanceRefProgId(Long balRefProgram);
}
