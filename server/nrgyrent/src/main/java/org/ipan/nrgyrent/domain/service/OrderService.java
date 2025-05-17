package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
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
public class OrderService {
    private final OrderRepo orderRepo;

    @Transactional(readOnly = true)
    public boolean haveEnoughTrxForOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = em.getReference(AppUser.class, command.getUserId());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // TODO: use group balance type in the future
        
        Balance balance = user.getBalance();
        if (balance == null) {
            throw new IllegalStateException("User balance not found");
        }

        return balance.getSunBalance() >= command.getSunAmount();
    }

    @Transactional
    public Order createPendingOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = em.getReference(AppUser.class, command.getUserId());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // TODO: use group balance type in the future
        
        Balance balance = user.getBalance();
        if (balance == null) {
            throw new IllegalStateException("User balance not found");
        }

        if (!haveEnoughTrxForOrder(command)) {
            throw new NotEnoughBalanceException("Not enough balance");
        }

        balance.setSunBalance(balance.getSunBalance() - command.getSunAmount());

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setDuration(command.getDuration());
        order.setCorrelationId(command.getCorrelationId());
        order.setEnergyAmount(command.getEnergyAmount());
        order.setSunAmount(command.getSunAmount());
        order.setItrxFeeSunAmount(command.getItrxFeeSunAmount());
        order.setReceiveAddress(command.getReceiveAddress());
        order.setSerial(command.getSerial());

        em.persist(order);

        return order;
    }

    @Transactional
    public Order completeOrder(AddOrUpdateOrderCommand command) {
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
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setItrxStatus(command.getItrxStatus());

        // TODO: Refund the order amount to the user
        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
