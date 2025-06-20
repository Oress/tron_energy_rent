package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;


@Component
@AllArgsConstructor
@Slf4j
public class OrderEventDomainListener {
    private final OrderService orderService;

    @EventListener
    @Order(1)
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);
        AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                .correlationId(event.getCorrelationId())
                .itrxStatus(event.getItrxStatus())
                .txId(event.getTxId())
                .serial(event.getSerial())
                .build();
        orderService.completeOrder(command);
    }

    @EventListener
    @Order(1)
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);
        AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                .correlationId(event.getCorrelationId())
                .itrxStatus(event.getItrxStatus())
                .serial(event.getSerial())
                .build();
        orderService.refundOrder(command);
    }
}
