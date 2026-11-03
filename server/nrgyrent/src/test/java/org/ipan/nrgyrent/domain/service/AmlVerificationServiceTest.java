package org.ipan.nrgyrent.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.google.gson.JsonObject;
import org.ipan.nrgyrent.domain.model.AmlRiskLevel;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationPaymentStatus;
import org.ipan.nrgyrent.domain.model.AmlVerificationStatus;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.netts.dto.NettsAmlStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    @ParameterizedTest
    @EnumSource(value = AmlVerificationStatus.class, names = {"PROCESSING", "COMPLETED"})
    void completesPendingPaymentEvenIfProviderAlreadyMarkedReportCompleted(AmlVerificationStatus providerStatus) {
        AmlVerification verification = new AmlVerification();
        verification.setId(42L);
        verification.setStatus(providerStatus);
        verification.setPaymentStatus(AmlVerificationPaymentStatus.PENDING);
        Balance balance = new Balance();
        balance.setSunBalance(1_000L);
        verification.setBalance(balance);
        verification.setPaidSun(250L);
        when(amlVerificationRepo.findById(42L)).thenReturn(Optional.of(verification));

        NettsAmlStatusResponse.DataResponse data = new NettsAmlStatusResponse.DataResponse();
        data.setStatus("completed");
        data.setRiskScore(10.0);
        data.setRiskLevel("low");
        data.setIsSanctioned(false);
        data.setMessage("Report completed");
        JsonObject providerResult = new JsonObject();
        providerResult.addProperty("risk_score", 10.0);
        data.setResult(providerResult);

        AmlVerification result = amlVerificationService.completeVerification(42L, data);

        assertSame(verification, result);
        assertEquals(AmlVerificationPaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(AmlVerificationStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(10.0, result.getRiskScore());
        assertEquals(AmlRiskLevel.LOW, result.getRiskLevel());
        assertEquals(false, result.getSanctioned());
        assertEquals("Report completed", result.getMessage());
        assertEquals(providerResult.toString(), result.getResult());
        assertEquals(1_000L, balance.getSunBalance());
    }

    @Test
    void leavesFullyCompletedVerificationUnchanged() {
        AmlVerification verification = new AmlVerification();
        verification.setId(42L);
        verification.setStatus(AmlVerificationStatus.COMPLETED);
        verification.setPaymentStatus(AmlVerificationPaymentStatus.COMPLETED);
        Instant completedAt = Instant.parse("2026-11-01T12:00:00Z");
        verification.setCompletedAt(completedAt);
        verification.setResult("original result");
        verification.setRiskScore(10.0);
        when(amlVerificationRepo.findById(42L)).thenReturn(Optional.of(verification));
        NettsAmlStatusResponse.DataResponse data = new NettsAmlStatusResponse.DataResponse();
        data.setRiskScore(20.0);

        AmlVerification result = amlVerificationService.completeVerification(42L, data);

        assertSame(verification, result);
        assertEquals(AmlVerificationPaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(AmlVerificationStatus.COMPLETED, result.getStatus());
        assertEquals(completedAt, result.getCompletedAt());
        assertEquals("original result", result.getResult());
        assertEquals(10.0, result.getRiskScore());
    }
}
