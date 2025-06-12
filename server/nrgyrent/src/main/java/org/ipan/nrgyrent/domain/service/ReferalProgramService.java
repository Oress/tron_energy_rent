package org.ipan.nrgyrent.domain.service;

import java.util.List;
import java.util.UUID;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceReferralProgramRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralProgramRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class ReferalProgramService {
    private final BalanceReferralProgramRepo balanceReferralProgramRepo;
    private final ReferralProgramRepo referralProgramRepo;
    private final AppUserRepo userRepo;

    @Transactional
    public BalanceReferralProgram createReferalProgramForUser(Long userId, Long refProgramId) {
        AppUser targetUser = userRepo.findById(userId).orElse(null);
        if (targetUser == null) {
            logger.error("User with id {} not found", userId);
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

        List<BalanceReferralProgram> exisitingPrograms = balanceReferralProgramRepo.findByBalanceId(targetUser.getBalance().getId());
        BalanceReferralProgram brp;
        if (!exisitingPrograms.isEmpty()) {
            brp = exisitingPrograms.get(0);

            logger.info("Updating EXISITING referral program for the user {}, FROM {} TO refProgId: {}", userId, brp.getReferralProgram().getId(), refProgramId);

            brp.setReferralProgram(referralProgram);
        } else {
            logger.info("Creating NEW referral program for the user {}, refProgId: {}", userId, refProgramId);

            brp = new BalanceReferralProgram();
            brp.setBalance(targetUser.getBalance());
            brp.setLink(userId.toString());
            brp.setReferralProgram(referralProgram);

            balanceReferralProgramRepo.save(brp);
        }

        return brp;
    }

    @Transactional
    public ReferralProgram createReferalProgramSetup(String label, Integer percentage) {
/*         if (Boolean.FALSE.equals(referralProgram.getActive())) {
            logger.error("Cannot change individual tariff for user {}: tariff {} is inactive", appUser.getTelegramId(), refProgramId);
            throw new CannotChangeIndividualTariff("Cannot change individual tariff for user with inactive tariff.");
        } */

        if (label == null || label.isBlank()) {
            logger.info("Label is blank when creating ref. program {}", label);
            throw new IllegalArgumentException("Label is blank when creating ref. program {}");
        }

        if (label.length() < 3) {
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
}
