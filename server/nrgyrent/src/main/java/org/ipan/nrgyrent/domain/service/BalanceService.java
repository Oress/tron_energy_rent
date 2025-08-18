package org.ipan.nrgyrent.domain.service;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.LiquibaseParameters;
import org.ipan.nrgyrent.domain.exception.InvalidAdjustedBalanceException;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.exception.UserAlreadyHasGroupBalanceException;
import org.ipan.nrgyrent.domain.exception.UserIsDisabledException;
import org.ipan.nrgyrent.domain.exception.UserIsManagerException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.exception.UsersMustBelongToTheSameGroupException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.tron.node.events.AddressesWatchlist;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class BalanceService {
    private final AddressesWatchlist addressesWatchlist; // ideally, communicate via events
    private final TariffRepo tariffRepo;
    private final AppUserRepo userRepo;
    private final BalanceRepo balanceRepo;
    private final ManualBalanceAdjustmentActionRepo manualBalanceAdjustmentActionRepo;
    private final ManagedWalletService managedWalletService;
    private final ReferalProgramService referalProgramService;
    private final LiquibaseParameters liquibaseParameters;
    private final BalanceReferralProgramRepo balanceReferralProgramRepo;
    private final NrgConfigsService nrgConfigsService;

    @Transactional
    public Balance removeUsersFromTheGroupBalance(Long balanceId, List<TgUserId> userInfos) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for removing users: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for removing users");
        }

        List<Long> userIds = userInfos.stream().map(i -> i.getId()).toList();
        List<AppUser> usersToRemove = userRepo.findAllById(userIds);
        if (usersToRemove.isEmpty() || usersToRemove.size() != userInfos.size()) {
            logger.info("Some users are not registered: {}", userInfos);
            List<TgUserId> notRegisteredUserId = userInfos.stream()
                    .filter(info -> usersToRemove.stream().noneMatch(user -> user.getTelegramId() == info.getId()))
                    .toList();
            throw new UserNotRegisteredException(notRegisteredUserId, "Some users are not registered: " + notRegisteredUserId);
        }

        // Check that all users belong to the same group balance
        if (usersToRemove.stream().anyMatch(user -> user.getGroupBalance() == null
                || !user.getGroupBalance().getId().equals(balanceId))) {
            logger.info("Some users are not in the group: {}", usersToRemove);
            throw new UsersMustBelongToTheSameGroupException("Some users are not in the group");
        }

        for (AppUser userToRemove : usersToRemove) {
            if (userToRemove.isGroupManager()) {
                balance.setManager(null);
            }
            userToRemove.setGroupBalance(null);
            referalProgramService.createReferalProgramForUser(userToRemove.getTelegramId(), liquibaseParameters.getDefaultRefProgramId());
        }

        return balance;
    }

    @Transactional
    public Balance addUsersToTheGroupBalance(Long balanceId, List<TgUserId> userInfos) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for adding users: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for adding users");
        }

        List<Long> userIds = userInfos.stream().map(i -> i.getId()).toList();
        List<AppUser> usersToAdd = userRepo.findAllById(userIds);
        if (usersToAdd.isEmpty() || usersToAdd.size() != userInfos.size()) {
            logger.info("Some users are not registered: {}", userInfos);
            List<TgUserId> notRegisteredUserId = userInfos.stream()
                    .filter(info -> usersToAdd.stream().noneMatch(user -> user.getTelegramId() == info.getId()))
                    .toList();
            throw new UserNotRegisteredException(notRegisteredUserId, "Some users are not registered: " + notRegisteredUserId);
        }

        for (AppUser userToAdd : usersToAdd) {
            if (userToAdd.getDisabled()) {
                logger.error("User cannot be assigned as manager because they are DISABLED, user: {}", userToAdd.getTelegramId());
                throw new UserIsDisabledException("member in another group {}");
            }

            if (userToAdd.isInGroup() && !balanceId.equals(userToAdd.getGroupBalance().getId())) {
                logger.error("User cannot be assigned as manager because they are member in another group, user: {}, group {}", userToAdd.getTelegramId(), userToAdd.getGroupBalance().getIdAndLabel());
                throw new UserAlreadyHasGroupBalanceException("User is already belongs to another group balance: " + userToAdd.getTelegramId());
            }

            userToAdd.setGroupBalance(balance);
            referalProgramService.removeRefProgram(userToAdd.getTelegramId());
        }
        return balance;
    }

    @Transactional
    public Balance renameGroupBalance(Long balanceId, String newLabel) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for renaming: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for renaming");
        }
        // label should be > 3 characters
        if (newLabel.length() < 3) {
            logger.info("New label is too short: {}", newLabel);
            throw new IllegalArgumentException("New label is too short");
        }

        balance.setLabel(newLabel);
        return balance;
    }

    @Transactional
    public Balance adjustWithdrawLimit(Long balanceId, Long totalLimit) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for adjusting limit: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for adjusting limit");
        }
        // amountSun should be > 0
        if (totalLimit < 0) {
            logger.warn("New amount is negative: {}", totalLimit);
            throw new InvalidAdjustedBalanceException("New amount is negative");
        }

        balance.setDailyWithdrawalLimitSun(totalLimit);
        balance.setDailyWithdrawalRemainingSun(totalLimit);
        return balance;
    }

    @Transactional
    public Balance adjustBalance(Long balanceId, Long amountSun, Long createdBy) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for adjusting: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for adjusting");
        }

        AppUser referenceById = userRepo.getReferenceById(createdBy);

        // amountSun should be > 0
        if (amountSun < 0) {
            logger.warn("New amount is negative: {}", amountSun);
            throw new InvalidAdjustedBalanceException("New amount is negative");
        }

        ManualBalanceAdjustmentAction action = createManualBalanceAdjustmentAction(referenceById, balance, amountSun);
        manualBalanceAdjustmentActionRepo.save(action);

        balance.setSunBalance(amountSun);
        return balance;
    }

    private ManualBalanceAdjustmentAction createManualBalanceAdjustmentAction(AppUser changedBy, Balance balance,
            Long amountTo) {
        ManualBalanceAdjustmentAction action = new ManualBalanceAdjustmentAction();
        action.setBalance(balance);
        action.setAmountFrom(balance.getSunBalance());
        action.setAmountTo(amountTo);
        action.setChangedBy(changedBy);
        return action;
    }

    @Transactional
    public Balance createGroupBalance(String label, TgUserId tgUserId, Long tariffId) {
        // 1. Check that users are registered.
        Long managerId = tgUserId.getId();
        AppUser manager = userRepo.findById(managerId).orElse(null);

        if (manager == null) {
            logger.error("Manager is not registered: {}", managerId);
            throw new UserNotRegisteredException(List.of(tgUserId), "Manager is not registered");
        }

        if (manager.getDisabled()) {
            logger.error("Manager is not registered: {}", managerId);
            throw new UserIsDisabledException("Manager is disabled");
        }

        // 2. Check that manager is not already in the group balance.
        if (manager.isInGroup()) {
            logger.info("Manager is already in a group: {}", managerId);
            throw new UserIsManagerException("Manager is already in a group");
        }

        // 3. Create group balance and add users to it.
        ManagedWallet depositWallet = generateDepositWallet();

        Balance balance = new Balance();
        balance.setLabel(label);
        balance.setEnergyProvider(nrgConfigsService.readCurrentProviderConfig());
        balance.setType(BalanceType.GROUP);
        balance.setManager(manager);
        balance.setDepositAddress(depositWallet.getBase58Address());
        setDefaultWithdrawLimits(balance);

        balance.setTariff(getTariffOrDefault(tariffId));

        balanceRepo.save(balance);

        addressesWatchlist.addAddress(balance.getDepositAddress());

        manager.setGroupBalance(balance);
        referalProgramService.removeRefProgram(managerId);

        return balance;
    }

    @Transactional
    public Balance createIndividualBalance(AppUser user, CreateUserCommand command) {
        ManagedWallet depositWallet = generateDepositWallet();

        Balance balance = new Balance();
        balance.setType(BalanceType.INDIVIDUAL);
        balance.setDepositAddress(depositWallet.getBase58Address());
        balance.setEnergyProvider(nrgConfigsService.readCurrentProviderConfig());

        balance.setTariff(getTariffOrDefault(command.getTariffId()));
        setDefaultWithdrawLimits(balance);

        String link = command.getRefferalLink();
        if (link != null && !link.isEmpty()) {
            BalanceReferralProgram refferalProgram = balanceReferralProgramRepo.findByLink(link);

            if (refferalProgram != null) {
                balance.setReferralProgram(refferalProgram);
            } else {
                logger.error("Cannot find refferal program by link {} user id {} login {}", link, command.getTelegramId(), command.getUsername());
            }
        }

        balanceRepo.save(balance);
        addressesWatchlist.addAddress(balance.getDepositAddress());

        user.setBalance(balance);

        return balance;
    }

    @Retryable
    @SneakyThrows
    private ManagedWallet generateDepositWallet() {
        EntityManager em = getEntityManager();
        ManagedWallet depositWallet = managedWalletService.generateManagedWallet();
        em.persist(depositWallet);
        return depositWallet;
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }

    @Transactional
    public void deactivateGroupBalance(Long balanceId) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for deletion: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for deletion");
        }

        balance.setIsActive(false);
        balance.setManager(null);
        userRepo.findAllByGroupBalanceId(balanceId).forEach(user -> {
            user.setGroupBalance(null);
            referalProgramService.createReferalProgramForUser(user.getTelegramId(), liquibaseParameters.getDefaultRefProgramId());
        });
    }

    @Transactional
    public void deactivateUserBalance(Long balanceId) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for deletion: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for deletion");
        }

        balance.setIsActive(false);
    }

    @Transactional
    public void subtractSunBalance(Balance targetBalance, Long sunAmount) {
        if (targetBalance == null) {
            throw new IllegalArgumentException("Balance not found");
        }

        if (targetBalance.getSunBalance() < sunAmount) {
            throw new NotEnoughBalanceException("Not enough balance");
        }

        targetBalance.setSunBalance(targetBalance.getSunBalance() - sunAmount);
    }

    @Transactional
    public void subtractWithdrawLimit(Balance targetBalance, long sunAmount) {
        if (targetBalance == null) {
            throw new IllegalArgumentException("Balance not found");
        }

        if (targetBalance.getDailyWithdrawalRemainingSun() < sunAmount) {
            throw new NotEnoughBalanceException("Not enough limit");
        }

        targetBalance.setDailyWithdrawalRemainingSun(targetBalance.getDailyWithdrawalRemainingSun() - sunAmount);
    }

    @Transactional
    public void refundWithdrawLimit(Balance targetBalance, long sunAmount) {
        if (targetBalance == null) {
            throw new IllegalArgumentException("Balance not found");
        }

        targetBalance.setDailyWithdrawalRemainingSun(targetBalance.getDailyWithdrawalRemainingSun() + sunAmount);
    }

    @Transactional
    public void changeManager(Long selectedBalanceId, TgUserId tgUserId) {
        Long newManagerId = tgUserId.getId();
        Balance selectedBalance = balanceRepo.findById(selectedBalanceId).orElse(null);
        if (selectedBalance == null) {
            logger.error("Balance not found for changing manager: {}", selectedBalanceId);
            throw new IllegalArgumentException("Balance not found for changing manager");
        }

        AppUser newManager = userRepo.findById(newManagerId).orElse(null);
        if (newManager == null) {
            logger.error("User not found for changing manager: {}", newManagerId);
            throw new UserNotRegisteredException(List.of(tgUserId), "User not found for changing manager");
        }

        if (newManager.getDisabled()) {
            logger.error("User cannot be assigned as manager because they are DISABLED, user: {}", newManagerId);
            throw new UserIsDisabledException("member in another group {}");
        }

        if (newManager.isInGroup() && !selectedBalanceId.equals(newManager.getGroupBalance().getId())) {
            logger.error("User cannot be assigned as manager because they are member in another group, user: {}, group {}", newManagerId, newManager.getGroupBalance().getIdAndLabel());
            throw new UserAlreadyHasGroupBalanceException("member in another group {}");
        }

        referalProgramService.removeRefProgram(newManagerId);
        selectedBalance.setManager(newManager);
        newManager.setGroupBalance(selectedBalance);
    }

    private Tariff getTariffOrDefault(Long tariffId) {
        Tariff result = null;
        if (tariffId != null) {
            result = tariffRepo.findById(tariffId).orElse(null);
        }
        if (result == null) {
            result = tariffRepo.getDefaultTariff();
        }
        return result;
    }

    private void setDefaultWithdrawLimits(Balance balance) {
        balance.setDailyWithdrawalLimitSun(liquibaseParameters.getDefaultBalanceDailyLimit());
        balance.setDailyWithdrawalRemainingSun(liquibaseParameters.getDefaultBalanceDailyLimit());
    }
}
