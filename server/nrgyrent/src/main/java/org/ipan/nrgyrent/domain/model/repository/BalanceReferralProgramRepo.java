package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;

import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceReferralProgramRepo extends JpaRepository<BalanceReferralProgram, Long> {
    BalanceReferralProgram findByLink(String link);

    List<BalanceReferralProgram> findByBalanceId(Long balanceId);
}
