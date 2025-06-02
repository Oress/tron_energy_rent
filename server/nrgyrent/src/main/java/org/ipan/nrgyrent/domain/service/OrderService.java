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
            logger.error("User is not found when creating pending order, command {}", command);
            throw new IllegalArgumentException("User not found");
        }

        if (command.getDuration() == null || command.getEnergyAmountPerTx() == null || command.getSunAmountPerTx() == null || command.getTxAmount() == null
            || command.getCorrelationId() == null || command.getReceiveAddress() == null ||command.getItrxFeeSunAmount() == null) {
                logger.error("Some of the command properties are not set, command {}", command);
                throw new IllegalArgumentException("Some of the command properties are not set");
        }


        Long totalSunAmount = command.getTxAmount() * command.getSunAmountPerTx();
        Integer totalEnergyAmount = command.getTxAmount() * command.getEnergyAmountPerTx();

        Balance targetBalance = user.getBalanceToUse();
        balanceService.subtractSunBalance(targetBalance, totalSunAmount);

        logger.info("Creating a pending order for user id {} username: {} balance: {} params: {}", user.getTelegramId(), user.getTelegramUsername(), targetBalance.getId(), command);

        Order order = new Order();
        order.setUser(user);
        order.setBalance(targetBalance);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setDuration(command.getDuration());
        order.setCorrelationId(command.getCorrelationId());
        order.setTxAmount(command.getTxAmount());
        order.setSunAmount(totalSunAmount);
        order.setEnergyAmount(totalEnergyAmount);
        order.setItrxFeeSunAmount(command.getItrxFeeSunAmount());
        order.setReceiveAddress(command.getReceiveAddress());

        em.persist(order);

        return order;
    }

    @Transactional
    public Order completeOrder(AddOrUpdateOrderCommand command) {
        logger.info("Completing domain order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            logger.error("Order not found: {}", command.getCorrelationId());
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setItrxStatus(command.getItrxStatus());
        order.setTxId(command.getTxId());
        order.setSerial(command.getSerial());

        return order;
    }

    @Transactional
    public Order refundOrder(AddOrUpdateOrderCommand command) {
        logger.info("Refunding order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            logger.error("Order not found: {}", command.getCorrelationId());
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setItrxStatus(command.getItrxStatus());
        order.setSerial(command.getSerial());

        Balance balance = order.getBalance();
        balance.setSunBalance(balance.getSunBalance() + order.getSunAmount());

        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
