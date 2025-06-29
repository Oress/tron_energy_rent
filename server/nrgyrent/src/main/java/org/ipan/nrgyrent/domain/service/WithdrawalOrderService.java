package org.ipan.nrgyrent.domain.service;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.exception.NotManagerException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.WithdrawalOrder;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.model.repository.WithdrawalOrderRepo;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class WithdrawalOrderService {
    private final UserRepo userRepo;
    private final WithdrawalOrderRepo withdrawalOrderRepo;
    private final BalanceService balanceService;

    @Transactional
    public WithdrawalOrder createPendingOrder(Long userId, Long amountSun, Long feeAmountSun, String receiveAddress) {
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            logger.error("User is not found id: {}", userId);
            throw new IllegalArgumentException("User not found");
        }

        if (user.isInGroup() && !user.isGroupManager()) {
            logger.error("Member of a group tries to withdraw. userId {}, group: {}, amount: {}, receiveAddres {}", userId, user.getGroupBalance().getIdAndLabel(), amountSun, receiveAddress);
            throw new NotManagerException("User is not manager");
        }

        Balance targetBalance = user.getBalanceToUse();
        balanceService.subtractSunBalance(targetBalance, amountSun + feeAmountSun);
        balanceService.subtractWithdrawLimit(targetBalance, amountSun);
        logger.info("User {} withdrawing {} to {}", userId,  amountSun, receiveAddress);

        WithdrawalOrder order = new WithdrawalOrder();
        order.setUser(user);
        order.setBalance(targetBalance);
        order.setStatus(WithdrawalStatus.PENDING);
        order.setSunAmount(amountSun);
        order.setFeeSunAmount(feeAmountSun);
        order.setReceiveAddress(receiveAddress);

        withdrawalOrderRepo.save(order);
        logger.info("Creating withdrawal request id {} user id {} amount {} balance id {} receive wallet {}", order.getId(), userId, amountSun, targetBalance.getId(), receiveAddress);

        return order;
    }

    @Transactional
    public WithdrawalOrder completeOrder(Long withdrawalOrderId, String txId) {
        Optional<WithdrawalOrder> order = withdrawalOrderRepo.findById(withdrawalOrderId);

        if (order.isEmpty()) {
            logger.error("Order is not found id: {}", withdrawalOrderId);
            throw new IllegalArgumentException("Order not found");
        }

        WithdrawalOrder withdrawalOrder = order.get();
        withdrawalOrder.setStatus(WithdrawalStatus.COMPLETED);
        withdrawalOrder.setTxId(txId);


        logger.info("Completing withdrawal order id {} tx id {}", withdrawalOrderId, txId);

        return withdrawalOrder;
    }

    @Transactional
    public WithdrawalOrder refundOrder(Long withdrawalOrderId) {
        logger.info("Refunding order: {}", withdrawalOrderId);
        Optional<WithdrawalOrder> order = withdrawalOrderRepo.findById(withdrawalOrderId);

        if (order.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        WithdrawalOrder withdrawalOrder = order.get();
        if (withdrawalOrder.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Order is not in a refundable state");
        }

        withdrawalOrder.setStatus(WithdrawalStatus.FAILED);

        Balance balance = withdrawalOrder.getBalance();
        balance.setSunBalance(balance.getSunBalance() + withdrawalOrder.getSunAmount() + withdrawalOrder.getFeeSunAmount());
        balanceService.refundWithdrawLimit(balance, withdrawalOrder.getSunAmount());

        return withdrawalOrder;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
