package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepo orderRepo;
    private final BalanceService balanceService;

    @Transactional
    public Order createPendingOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = em.getReference(AppUser.class, command.getUserId());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Balance targetBalance = command.getUseGroupWallet() ? user.getGroupBalance() : user.getBalance();
        balanceService.subtractSunBalance(targetBalance, command.getSunAmount());

        Order order = new Order();
        order.setUser(user);
        order.setBalance(targetBalance);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setDuration(command.getDuration());
        order.setCorrelationId(command.getCorrelationId());
        order.setEnergyAmount(command.getEnergyAmount());
        order.setSunAmount(command.getSunAmount());
        order.setItrxFeeSunAmount(command.getItrxFeeSunAmount());
        order.setReceiveAddress(command.getReceiveAddress());

        em.persist(order);

        return order;
    }

    @Transactional
    public Order completeOrder(AddOrUpdateOrderCommand command) {
        logger.info("Completing order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setItrxStatus(command.getItrxStatus());
        order.setTxId(command.getTxId());

        return order;
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

        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
