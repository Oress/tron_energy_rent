package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class OrderEventDomainListener {
    private final OrderService orderService;

    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);
        AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                .correlationId(event.getCorrelationId())
                .itrxStatus(event.getItrxStatus())
                .txId(event.getTxId())
                .build();
        orderService.completeOrder(command);
    }

    @EventListener
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);
        AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                .correlationId(event.getCorrelationId())
                .itrxStatus(event.getItrxStatus())
                .build();
        orderService.refundOrder(command);
    }
}
