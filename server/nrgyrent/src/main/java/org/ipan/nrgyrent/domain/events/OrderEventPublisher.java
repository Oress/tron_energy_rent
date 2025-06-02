package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrderEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCompletedEvent(String correlationId, Integer itrxStatus, String txId, String serial) {
        eventPublisher.publishEvent(new OrderCompletedEvent(this, correlationId, itrxStatus, txId, serial));
    }

    public void publishOrderFailedEvent(String correlationId, Integer itrxStatus, String serial) {
        eventPublisher.publishEvent(new OrderFailedEvent(this, correlationId, itrxStatus, serial));
    }
}
