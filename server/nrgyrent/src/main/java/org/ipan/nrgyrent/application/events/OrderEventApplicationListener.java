package org.ipan.nrgyrent.application.events;

import org.ipan.nrgyrent.domain.events.OrderCompletedEvent;
import org.ipan.nrgyrent.domain.events.OrderFailedEvent;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class OrderEventApplicationListener {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final OrderRepo orderRepo;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final AutoDelegationViews autoDelegationViews;

    @EventListener
    @Transactional(readOnly = true)
    @org.springframework.core.annotation.Order(10)
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);

        Order order = orderRepo.findByCorrelationId(event.getCorrelationId()).orElse(null);
        if (order == null) {
            logger.error("Order not found for correlationId: {}", event.getCorrelationId());
            return;
        }
        UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
        if (order.getMessageToUpdate() == null) {
            logger.info("Order has no message id (AUTODELEGATION): correlationId {}", event.getCorrelationId());
            AutoDelegationSession session = autoDelegationSessionRepo.findSessionByOrderId(order.getId());
            autoDelegationViews.updateSessionStatus(userState, session);
        } else {
            telegramMessages.sendTransactionSuccessNotification(userState, order);
        }
    }

    @EventListener
    @Transactional(readOnly = true)
    @org.springframework.core.annotation.Order(10)
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);

        Order order = orderRepo.findByCorrelationId(event.getCorrelationId()).orElse(null);
        if (order == null) {
            logger.error("Order not found for correlationId: {}", event.getCorrelationId());
            return;
        }

        UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
        if (order.getMessageToUpdate() == null) {
            logger.info("Order has no message id (AUTODELEGATION): correlationId {}", event.getCorrelationId());
            AutoDelegationSession session = autoDelegationSessionRepo.findSessionByOrderId(order.getId());
            autoDelegationViews.updateSessionStatus(userState, session);
        } else {
            telegramMessages.sendTransactionRefundNotification(userState, order);
        }

    }
}
