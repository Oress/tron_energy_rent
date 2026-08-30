package org.ipan.nrgyrent.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationPaymentStatus;
import org.ipan.nrgyrent.domain.model.AmlVerificationStatus;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.netts.dto.NettsAmlStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmlVerificationServiceTest {
    @Mock
    private AmlVerificationRepo amlVerificationRepo;

    private AmlVerificationService amlVerificationService;

    @BeforeEach
    void setUp() {
        amlVerificationService = new AmlVerificationService(amlVerificationRepo, null, null);
    }

    @Test
    void skipVerificationRefundsPaymentAndStoresProviderOutcome() {
        Balance balance = new Balance();
        balance.setId(7L);
        balance.setSunBalance(1_000L);

        AmlVerification verification = new AmlVerification();
        verification.setId(42L);
        verification.setBalance(balance);
        verification.setPaidSun(250L);
        verification.setPaymentStatus(AmlVerificationPaymentStatus.PENDING);
        verification.setStatus(AmlVerificationStatus.PROCESSING);

        NettsAmlStatusResponse.DataResponse data = new NettsAmlStatusResponse.DataResponse();
        data.setStatus("skipped");
        data.setMessage("Address has no activity on blockchain. No charges applied.");

        when(amlVerificationRepo.findById(42L)).thenReturn(Optional.of(verification));

        AmlVerification result = amlVerificationService.skipVerification(42L, data);

        assertSame(verification, result);
        assertEquals(AmlVerificationPaymentStatus.REFUNDED, result.getPaymentStatus());
        assertEquals(AmlVerificationStatus.SKIPPED, result.getStatus());
        assertEquals("Address has no activity on blockchain. No charges applied.", result.getMessage());
        assertEquals(1_250L, result.getBalance().getSunBalance());
    }
}
