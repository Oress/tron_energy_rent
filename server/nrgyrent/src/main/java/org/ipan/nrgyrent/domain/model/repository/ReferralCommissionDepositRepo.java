package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.ReferralCommissionDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralCommissionDepositRepo extends JpaRepository<ReferralCommissionDeposit, Long> {
}
