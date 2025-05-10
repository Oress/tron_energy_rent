package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
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

    @Transactional
    public Order createPendingOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = em.getReference(AppUser.class, command.getUserId());

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setDuration(command.getDuration());
        order.setCorrelationId(command.getCorrelationId());
        order.setEnergyAmount(command.getEnergyAmount());
        order.setTrxAmount(command.getTrxAmount());
        order.setReceiveAddress(command.getReceiveAddress());
        order.setSerial(command.getSerial());

        em.persist(order);

        return order;
    }

    @Transactional
    public Order completeOrder(AddOrUpdateOrderCommand command) {
        Optional<Order> bySerial = orderRepo.findBySerial(command.getSerial());

        if (bySerial.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = bySerial.get();
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setItrxStatus(command.getItrxStatus());
        order.setTxId(command.getTxId());

        return order;
    }

    @Transactional
    public Order refundOrder(AddOrUpdateOrderCommand command) {
        Optional<Order> bySerial = orderRepo.findBySerial(command.getSerial());

        if (bySerial.isEmpty()) {
            throw new IllegalArgumentException("Order not found");
        }

        Order order = bySerial.get();
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
