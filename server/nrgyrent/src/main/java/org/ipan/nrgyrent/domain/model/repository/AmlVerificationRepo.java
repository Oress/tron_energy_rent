package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmlVerificationRepo extends JpaRepository<AmlVerification, Long> {
    List<AmlVerification> findAllByStatus(AmlVerificationStatus status);
    List<AmlVerification> findAllByBalanceIdOrderByCreatedAtDesc(Long balanceId);
}
