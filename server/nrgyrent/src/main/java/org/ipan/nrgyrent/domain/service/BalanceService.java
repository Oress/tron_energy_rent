package org.ipan.nrgyrent.domain.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.exception.UserAlreadyHasGroupBalanceException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.ManualBalanceAdjustmentAction;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.ManualBalanceAdjustmentActionRepo;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
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
    private final UserRepo userRepo;
    private final BalanceRepo balanceRepo;
    private final ManualBalanceAdjustmentActionRepo manualBalanceAdjustmentActionRepo;
    private final ManagedWalletService managedWalletService;

    @Transactional
    public Balance removeUsersFromTheGroupBalance(Long balanceId, List<Long> userIds) {
        Balance balance = balanceRepo.findByIdWithUsers(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for removing users: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for removing users");
        }

        // Check if users are already in the group
        List<AppUser> usersToRemove = userRepo.findAllById(userIds);
        if (usersToRemove.isEmpty() || usersToRemove.size() != userIds.size()) {
            logger.info("Some users are not registered: {}", userIds);
            throw new UserNotRegisteredException("Some users are not registered");
        }

        if (usersToRemove.stream().anyMatch(user -> user.getGroupBalance() != null && !balanceId.equals(user.getGroupBalance().getId()))) {
            logger.info("Some users are already in the group: {}", usersToRemove);
            throw new UserAlreadyHasGroupBalanceException("Some users already have a group balance");
        }

        Map<Long, AppUser> usersToRemoveMap = usersToRemove.stream().collect(Collectors.toMap(AppUser::getTelegramId, Function.identity()));
        /* if (usersToAdd.isEmpty()) {
            logger.info("0 new users to add to group: {}", balanceId);
            throw new IllegalArgumentException("0 new users to add to group");
        } */

        balance.getUsers().removeIf(user -> usersToRemoveMap.containsKey(user.getTelegramId()));
        usersToRemove.forEach(nullableUser -> nullableUser.setGroupBalance(null));
        // Check if the group is empty after removing users
        return balance;
    }

    @Transactional
    public Balance addUsersToTheGroupBalance(Long balanceId, List<Long> userIds) {
        Balance balance = balanceRepo.findByIdWithUsers(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for adding users: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for adding users");
        }

        // Check if users are already in the group
        List<AppUser> usersToAdd = userRepo.findAllById(userIds);
        if (usersToAdd.isEmpty() || usersToAdd.size() != userIds.size()) {
            logger.info("Some users are not registered: {}", userIds);
            throw new UserNotRegisteredException("Some users are not registered");
        }

        if (usersToAdd.stream().anyMatch(user -> user.getGroupBalance() != null && !balanceId.equals(user.getGroupBalance().getId()))) {
            logger.info("Some users are already in the group: {}", usersToAdd);
            throw new UserAlreadyHasGroupBalanceException("Some users already have a group balance");
        }

         usersToAdd.removeIf(user -> balance.getUsers().contains(user));
        /* if (usersToAdd.isEmpty()) {
            logger.info("0 new users to add to group: {}", balanceId);
            throw new IllegalArgumentException("0 new users to add to group");
        } */

        // Add users to the group
        balance.getUsers().addAll(usersToAdd);
        usersToAdd.forEach(nullableUser -> {
            nullableUser.setGroupBalance(balance);
        });
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
    public Balance adjustBalance(Long balanceId, Long amountSun, Long createdBy) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);
        if (balance == null) {
            logger.error("Balance not found for adjusting: {}", balanceId);
            throw new IllegalArgumentException("Balance not found for adjusting");
        }

        AppUser referenceById = userRepo.getReferenceById(createdBy);;

        // amountSun should be > 0
        if (amountSun < 0) {
            logger.info("New amount is negative: {}", amountSun);
            throw new IllegalArgumentException("New amount is negative");
        }

        ManualBalanceAdjustmentAction action = createManualBalanceAdjustmentAction(referenceById, balance, amountSun);
        manualBalanceAdjustmentActionRepo.save(action);

        balance.setSunBalance(amountSun);
        return balance;
    }

    private ManualBalanceAdjustmentAction createManualBalanceAdjustmentAction(AppUser changedBy, Balance balance, Long amountTo) {
        ManualBalanceAdjustmentAction action = new ManualBalanceAdjustmentAction();
        action.setBalance(balance);
        action.setAmountFrom(balance.getSunBalance());
        action.setAmountTo(amountTo);
        action.setChangedBy(changedBy);
        return action;
    }

    @Transactional
    public Balance createGroupBalance(String label, List<Long> userIds) {
        // 1. Check that users are registered.
        List<AppUser> registeredUsers = userRepo.findAllById(userIds);

        if (registeredUsers.isEmpty() || registeredUsers.size() != userIds.size()) {
            logger.info("Some users are not registered: {}", userIds);
            throw new UserNotRegisteredException("Some users are not registered");
        }

        // 2. Check that users are not already in the group balance.
        List<AppUser> usersInGroup = registeredUsers.stream()
                .filter(user -> BalanceType.GROUP.equals(user.getBalance().getType())).toList();
        if (!usersInGroup.isEmpty()) {
            logger.info("Some users are already in the group: {}", usersInGroup);
            throw new UserAlreadyHasGroupBalanceException("Some users already have a group balance");
        }

        // 3. Create group balance and add users to it.
        ManagedWallet depositWallet = generateDepositWallet();

        Balance balance = new Balance();
        balance.setLabel(label);
        balance.setType(BalanceType.GROUP);
        balance.setDepositAddress(depositWallet.getBase58Address());

        balanceRepo.save(balance);

        List<AppUser> users = userRepo.findAllById(userIds);
        for (AppUser user : users) {
            user.setGroupBalance(balance);
        }

        return balance;
    }

    @Transactional
    public Balance createIndividualBalance(AppUser user) {
        ManagedWallet depositWallet = generateDepositWallet();

        Balance balance = new Balance();
        balance.setType(BalanceType.INDIVIDUAL);
        balance.setDepositAddress(depositWallet.getBase58Address());
        balanceRepo.save(balance);

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
        balance.getUsers().forEach(user -> {
            user.setGroupBalance(null);
        });
    }

}
