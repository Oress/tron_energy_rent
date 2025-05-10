package org.ipan.nrgyrent.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.events.OrderCompletedEvent;
import org.ipan.nrgyrent.domain.events.OrderFailedEvent;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.TelegramState;
import org.ipan.nrgyrent.telegram.UserState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class OrderEventApplicationListener {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final OrderRepo orderRepo;

    @EventListener
    @Transactional(readOnly = true)
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);

        Order order = orderRepo.findBySerial(event.getSerial()).orElse(null);
        if (order == null) {
            logger.error("Order not found for serial: {}", event.getSerial());
            return;
        }

        UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
        telegramMessages.sendTransactionSuccessNotification(userState);
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);

        Order order = orderRepo.findBySerial(event.getSerial()).orElse(null);
        if (order == null) {
            logger.error("Order not found for serial: {}", event.getSerial());
            return;
        }

        UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
        telegramMessages.sendTransactionRefundNotification(userState);
    }
}
