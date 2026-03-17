package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AmlVerificationRepo extends JpaRepository<AmlVerification, Long> {
    List<AmlVerification> findAllByPaymentStatus(AmlVerificationPaymentStatus paymentStatus);
    List<AmlVerification> findAllByBalanceIdOrderByCreatedAtDesc(Long balanceId);
}
