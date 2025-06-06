package org.ipan.nrgyrent.domain.service;

import java.util.UUID;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralProgramRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ReferalProgramService {
    private final ReferralProgramRepo referralProgramRepo;
    private final BalanceRepo balanceRepo;

    @Transactional
    public BalanceReferralProgram createReferalProgramForBalance(Long balanceId, Long refProgramId) {
        Balance targetBalance = balanceRepo.findById(balanceId).orElse(null);
        if (targetBalance == null) {
            logger.error("User with id {} not found", balanceId);
            throw new IllegalArgumentException("User with id {} not found");
        }

        ReferralProgram referralProgram = referralProgramRepo.findById(refProgramId).orElse(null);
        if (referralProgram == null) {
            logger.error("Tariff with id {} not found", refProgramId);
            throw new IllegalArgumentException("Tariff with id {} not found");
        }

/*         if (Boolean.FALSE.equals(referralProgram.getActive())) {
            logger.error("Cannot change individual tariff for user {}: tariff {} is inactive", appUser.getTelegramId(), refProgramId);
            throw new CannotChangeIndividualTariff("Cannot change individual tariff for user with inactive tariff.");
        } */

        // TODO: check that balance already has a referal program

        BalanceReferralProgram brp = new BalanceReferralProgram();
        brp.setBalance(targetBalance);
        brp.setLink(UUID.randomUUID().toString());
        brp.setReferralProgram(referralProgram);

        return brp;
    }

    @Transactional
    public ReferralProgram createReferalProgram(String label, Integer percentage) {
/*         if (Boolean.FALSE.equals(referralProgram.getActive())) {
            logger.error("Cannot change individual tariff for user {}: tariff {} is inactive", appUser.getTelegramId(), refProgramId);
            throw new CannotChangeIndividualTariff("Cannot change individual tariff for user with inactive tariff.");
        } */

        if (label == null || label.isBlank()) {
            logger.info("Label is blank when creating ref. program {}", label);
            throw new IllegalArgumentException("Label is blank when creating ref. program {}");
        }

        if (label.length() <= 3) {
            logger.info("Label is less than 3 symbols long when creating ref. program {}", label);
            throw new IllegalArgumentException("Label is less than 3 symbols long when creating ref. program {}");
        }

        if (percentage <= 0 || percentage >= 100) {
            logger.info("Ref program percentage must be in (0-100) range: percentage={}", percentage);
            throw new IllegalArgumentException("Ref program percentage must be in (0-100) range");
        }

        ReferralProgram refProgram = new ReferralProgram();
        refProgram.setLabel(label);
        refProgram.setPercentage(percentage);

        referralProgramRepo.save(refProgram);

        return refProgram;
    }

    @Transactional
    public ReferralProgram renameRefProgram(Long refProgramId, String newLabel) {
        logger.info("Changing label for ref. program ID {} to {}", refProgramId, newLabel);

        ReferralProgram referralProgram = referralProgramRepo.findById(refProgramId).orElse(null);
        if (referralProgram == null) {
            logger.error("ReferralProgram not found for renaming: {}", refProgramId);
            throw new IllegalArgumentException("ReferralProgram not found for renaming");
        }
        // label should be > 3 characters
        if (newLabel.length() < 3) {
            logger.info("New label is too short: {}", newLabel);
            throw new IllegalArgumentException("New label is too short");
        }

        referralProgram.setLabel(newLabel);
        logger.info("ReferralProgram label changed successfully: {}", referralProgram);
        return referralProgram;
    }

    @Transactional
    public ReferralProgram changePercentage(Long refProgramId, Integer percentage) {
        logger.info("Changing Percentage for ref. proftram ID {} to {}", refProgramId, percentage);
        ReferralProgram referralProgram = referralProgramRepo.findById(refProgramId).orElse(null);
        if (referralProgram == null) {
            logger.error("ReferralProgram not found for renaming: {}", refProgramId);
            throw new IllegalArgumentException("ReferralProgram not found for renaming");
        }

        if (percentage <= 0 || percentage >= 100) {
            logger.info("Ref program percentage must be in (0-100) range: percentage={}", percentage);
            throw new IllegalArgumentException("Ref program percentage must be in (0-100) range");
        }

        referralProgram.setPercentage(percentage);
        logger.info("ReferralProgram percentage changed successfully: id: {} label: {}", referralProgram.getId(), referralProgram.getLabel());
        return referralProgram;
    }
/* 
    @Transactional
    public void deactivateTariff(Long tariffId) {
        Tariff tariff = referralProgramRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff not found for deactivation: {}", tariffId);
            throw new IllegalArgumentException("Tariff not found for deactivation");
        }

        // Deactivate the tariff
        tariff.setActive(false);

        Tariff defaultTariff = referralProgramRepo.getDefaultTariff();
        if (defaultTariff == null) {
            logger.error("Default tariff not found, cannot set default tariff for balances");
            throw new IllegalStateException("Default tariff not found, cannot set default tariff for balances");
        }

        // Get All balances with this tariff, and set them to default tariff
        List<Balance> balances = balanceRepo.findAllByTariffId(tariffId);
        for (Balance balance : balances) {
            logger.info("Tariff Deactivated id: {}. Setting default tariff for balance ID: {}", tariffId, balance.getId());
            balance.setTariff(defaultTariff);
        }
    } */
}
