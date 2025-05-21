package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.WithdrawalOrder;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.model.repository.WithdrawalOrderRepo;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class WithdrawalOrderService {
    private final OrderRepo orderRepo;
    private final UserRepo userRepo;
    private final WithdrawalOrderRepo withdrawalOrderRepo;
    private final BalanceService balanceService;

    @Transactional
    public WithdrawalOrder createPendingOrder(Long userId, Boolean useGroup, Long amountSun, Long feeAmountSun, String receiveAddress) {
        AppUser user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Balance targetBalance = useGroup ? user.getGroupBalance() : user.getBalance();
        balanceService.subtractSunBalance(targetBalance, amountSun + feeAmountSun);

        WithdrawalOrder order = new WithdrawalOrder();
        order.setUser(user);
        order.setBalance(targetBalance);
        order.setStatus(WithdrawalStatus.PENDING);
        order.setSunAmount(amountSun);
        order.setFeeSunAmount(feeAmountSun);
        order.setReceiveAddress(receiveAddress);

        withdrawalOrderRepo.save(order);

        return order;
    }

    @Transactional
    public WithdrawalOrder completeOrder(Long withdrawalOrderId, String txId) {
        Optional<WithdrawalOrder> order = withdrawalOrderRepo.findById(withdrawalOrderId);

        if (order.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        WithdrawalOrder withdrawalOrder = order.get();
        withdrawalOrder.setStatus(WithdrawalStatus.COMPLETED);
        withdrawalOrder.setTxId(txId);

        return withdrawalOrder;
    }

    @Transactional
    public Order refundOrder(AddOrUpdateOrderCommand command) {
        logger.info("Refunding order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setItrxStatus(command.getItrxStatus());

        Balance balance = order.getBalance();
        balance.setSunBalance(balance.getSunBalance() + order.getSunAmount());

        // TODO: Refund the order amount to the user
        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
