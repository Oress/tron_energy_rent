package org.ipan.nrgyrent.domain.service;

import java.util.List;

import org.ipan.nrgyrent.domain.exception.CannotChangeGroupTariff;
import org.ipan.nrgyrent.domain.exception.CannotChangeIndividualTariff;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class TariffService {
    private final TariffRepo tariffRepo;
    private final AppUserRepo userRepo;
    private final BalanceRepo balanceRepo;

    @Transactional
    public void changeIndividualTariff(Long userId, Long tariffId) {
        AppUser appUser = userRepo.findById(userId).orElse(null);
        if (appUser == null) {
            logger.error("User with id {} not found", userId);
            return;
        }

        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff with id {} not found", tariffId);
            return;
        }

        Balance groupBalance = appUser.getGroupBalance();
        if (groupBalance != null) {
            logger.warn("User {} has a group balance, cannot change individual tariff for user.", appUser.getTelegramId());
            throw new CannotChangeIndividualTariff("Cannot change individual tariff for user with group balance.");
        }

        if (Boolean.FALSE.equals(tariff.getActive())) {
            logger.error("Cannot change individual tariff for user {}: tariff {} is inactive", appUser.getTelegramId(), tariffId);
            throw new CannotChangeIndividualTariff("Cannot change individual tariff for user with inactive tariff.");
        }

        Balance balance = appUser.getBalance();
        if (balance == null) {
            logger.error("User {} does not have an individual balance, cannot change tariff.", appUser.getTelegramId());
            return;
        }

        balance.setTariff(tariff);
    }

    @Transactional
    public void changeGroupTariff(Long balanceId, Long tariffId) {
        Balance groupBalance = balanceRepo.findById(balanceId).orElse(null);
        if (groupBalance == null) {
            logger.error("Group balance with id {} not found", balanceId);
            throw new CannotChangeGroupTariff("Group balance with id " + balanceId + " not found");
        }

        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff with id {} not found", tariffId);
            throw new CannotChangeGroupTariff("Tariff with id " + tariffId + " not found");
        }

        if (Boolean.FALSE.equals(tariff.getActive())) {
            logger.error("Cannot change group tariff for group balance {}: tariff {} is inactive", groupBalance.getId(), tariffId);
            throw new CannotChangeGroupTariff("Cannot change group tariff for inactive tariff.");
        }

        groupBalance.setTariff(tariff);
    }

    @Transactional
    public Tariff renameTariff(Long tariffId, String newLabel) {
        logger.info("Changing label for tariff ID {} to {}", tariffId, newLabel);

        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff not found for renaming: {}", tariffId);
            throw new IllegalArgumentException("Tariff not found for renaming");
        }
        // label should be > 3 characters
        if (newLabel.length() < 3) {
            logger.info("New label is too short: {}", newLabel);
            throw new IllegalArgumentException("New label is too short");
        }

        tariff.setLabel(newLabel);
        logger.info("Tariff label changed successfully: {}", tariff);
        return tariff;
    }

    @Transactional
    public Tariff changeTxType1Amount(Long tariffId, Long txType1Amount) {
        logger.info("Changing TX Type 1 amount for tariff ID {} to {}", tariffId, txType1Amount);
        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff not found for renaming: {}", tariffId);
            throw new IllegalArgumentException("Tariff not found for renaming");
        }
        // txType1Amount should be >= 0
        if (txType1Amount < 0) {
            logger.info("Transaction Type 1 amount must be non-negative: {}", txType1Amount);
            throw new IllegalArgumentException("Transaction Type 1 amount must be non-negative");
        }

        tariff.setTransactionType1AmountSun(txType1Amount);
        logger.info("Tariff TX Type 1 amount changed successfully: {}", tariff);
        return tariff;
    }

    @Transactional
    public Tariff changeTxType2Amount(Long tariffId, Long txType2Amount) {
        logger.info("Changing TX Type 2 amount for tariff ID {} to {}", tariffId, txType2Amount);
        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff not found for renaming: {}", tariffId);
            throw new IllegalArgumentException("Tariff not found for renaming");
        }
        // txType2Amount should be >= 0
        if (txType2Amount < 0) {
            logger.info("Transaction Type 2 amount must be non-negative: {}", txType2Amount);
            throw new IllegalArgumentException("Transaction Type 2 amount must be non-negative");
        }

        tariff.setTransactionType2AmountSun(txType2Amount);
        logger.info("Tariff TX Type 2 amount changed successfully: {}", tariff);
        return tariff;
    }

    @Transactional
    public Tariff createTariff(String label, Long txType1Amount, Long txType2Amount) {
        if (label.length() < 3) {
            logger.info("Label is too short: {}", label);
            throw new IllegalArgumentException("Label is too short");
        }

        if (txType1Amount < 0 || txType2Amount < 0) {
            logger.info("Transaction amounts must be non-negative: txType1Amount={}, txType2Amount={}", txType1Amount, txType2Amount);
            throw new IllegalArgumentException("Transaction amounts must be non-negative");
        }

        Tariff tariff = new Tariff();
        tariff.setLabel(label);
        tariff.setTransactionType1AmountSun(txType1Amount);
        tariff.setTransactionType2AmountSun(txType2Amount);

        tariffRepo.save(tariff);

        return tariff;
    }

    @Transactional
    public void deactivateTariff(Long tariffId) {
        Tariff tariff = tariffRepo.findById(tariffId).orElse(null);
        if (tariff == null) {
            logger.error("Tariff not found for deactivation: {}", tariffId);
            throw new IllegalArgumentException("Tariff not found for deactivation");
        }

        // Deactivate the tariff
        tariff.setActive(false);

        Tariff defaultTariff = tariffRepo.getDefaultTariff();
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
    }
}
