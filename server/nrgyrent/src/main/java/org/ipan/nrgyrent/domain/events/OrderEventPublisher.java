package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrderEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCompletedEvent(String correlationId, Integer itrxStatus, String txId) {
        eventPublisher.publishEvent(new OrderCompletedEvent(this, correlationId, itrxStatus, txId));
    }

    public void publishOrderFailedEvent(String correlationId, Integer itrxStatus) {
        eventPublisher.publishEvent(new OrderFailedEvent(this, correlationId, itrxStatus));
    }
}
