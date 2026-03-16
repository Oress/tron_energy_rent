package org.ipan.nrgyrent.domain.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.netts.dto.NettsAmlStatusResponse;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class AmlVerificationService {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final AmlVerificationRepo amlVerificationRepo;
    private final BalanceService balanceService;

    /**
     * Creates a pending AML verification, deducting the fee from the user's balance.
     * The caller is responsible for submitting the request to the external provider
     * and calling {@link #markProcessing(Long, String)} or {@link #refundVerification(Long)} afterwards.
     */
    @Transactional
    public AmlVerification createPendingVerification(Long userId, String walletAddress, AmlProvider provider) {
        EntityManager em = getEntityManager();

        AppUser user = em.find(AppUser.class, userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        Balance balance = user.getBalanceToUse();
        if (balance == null) {
            throw new IllegalArgumentException("User has no balance: " + userId);
        }

        Tariff tariff = user.getTariffToUse();
        if (tariff == null) {
            throw new IllegalArgumentException("User has no tariff: " + userId);
        }

        Long priceSun = tariff.getAmlCheckPriceSun();
        if (priceSun == null || priceSun <= 0) {
            throw new IllegalStateException("AML check price is not configured on tariff id: " + tariff.getId());
        }

        if (balance.getSunBalance() < priceSun) {
            throw new NotEnoughBalanceException("Not enough balance for AML check. Required: " + priceSun + ", available: " + balance.getSunBalance());
        }

        balanceService.subtractSunBalance(balance, priceSun);

        AmlVerification verification = new AmlVerification();
        verification.setBalance(balance);
        verification.setTariff(tariff);
        verification.setPaymentStatus(AmlVerificationPaymentStatus.PENDING);

        verification.setProvider(provider);
        verification.setPaidSun(priceSun);
        verification.setWalletAddress(walletAddress);


        em.persist(verification);

        logger.info("Created pending AML verification id: {} for user: {} wallet: {} price: {}",
                verification.getId(), userId, walletAddress, priceSun);
        return verification;
    }

    @Transactional
    public AmlVerification markProcessing(Long verificationId, String clientOrderId) {
        AmlVerification verification = amlVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("AML verification not found: " + verificationId));
        verification.setClientOrderId(clientOrderId);
        verification.setStatus(AmlVerificationStatus.PROCESSING);
        return verification;
    }

    @Transactional
    public AmlVerification completeVerification(Long verificationId, NettsAmlStatusResponse.DataResponse data) {
        AmlVerification verification = amlVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("AML verification not found: " + verificationId));

        if (AmlVerificationStatus.COMPLETED.equals(verification.getStatus())) {
            logger.warn("AML verification {} is already completed, skipping", verificationId);
            return verification;
        }

        verification.setStatus(AmlVerificationStatus.COMPLETED);
        verification.setRiskScore(data.getRiskScore());
        verification.setRiskLevel(parseRiskLevel(data.getRiskLevel()));
        verification.setSanctioned(data.getIsSanctioned());
        verification.setProvider(parseProvider(data.getProvider()));
        verification.setMessage(data.getMessage() != null ? data.getMessage() : null);

        if (data.getResult() != null) {
            verification.setResult(GSON.toJson(data.getResult()));
        }

        logger.info("Completed AML verification id: {} wallet: {} riskLevel: {} riskScore: {}",
                verificationId, verification.getWalletAddress(), data.getRiskLevel(), data.getRiskScore());
        return verification;
    }

    /**
     * Refunds the AML check fee back to the user's balance and marks the verification as refunded.
     * Safe to call multiple times — subsequent calls are no-ops.
     */
    @Transactional
    public AmlVerification refundVerification(Long verificationId) {
        AmlVerification verification = amlVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("AML verification not found: " + verificationId));

        if (AmlVerificationPaymentStatus.REFUNDED.equals(verification.getPaymentStatus())) {
            logger.warn("AML verification {} is already refunded, skipping", verificationId);
            return verification;
        }

        if (AmlVerificationPaymentStatus.COMPLETED.equals(verification.getPaymentStatus())) {
            logger.warn("AML verification {} has no payment to refund (paymentStatus: {}), skipping",
                    verificationId, verification.getPaymentStatus());
            return verification;
        }

        Balance balance = verification.getBalance();
        Long paidSun = verification.getPaidSun();
        if (balance != null && paidSun != null && paidSun > 0) {
            balance.setSunBalance(balance.getSunBalance() + paidSun);
            logger.info("Refunded {} sun to balance {} for AML verification {}", paidSun, balance.getId(), verificationId);
        }

        verification.setPaymentStatus(AmlVerificationPaymentStatus.REFUNDED);

        if (!AmlVerificationStatus.COMPLETED.equals(verification.getStatus())) {
            verification.setStatus(AmlVerificationStatus.FAILED);
        }

        return verification;
    }

    private AmlRiskLevel parseRiskLevel(String riskLevel) {
        if (riskLevel == null) return null;
        try {
            return AmlRiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown risk level from AML provider: {}", riskLevel);
            return null;
        }
    }

    private AmlProvider parseProvider(String provider) {
        if (provider == null) return null;
        try {
            return AmlProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown AML provider: {}", provider);
            return null;
        }
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
