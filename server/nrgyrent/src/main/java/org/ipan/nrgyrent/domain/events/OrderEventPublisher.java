package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrderEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCompletedEvent(String serial, Integer itrxStatus, String txId) {
        eventPublisher.publishEvent(new OrderCompletedEvent(this, serial, itrxStatus, txId));
    }

    public void publishOrderFailedEvent(String serial, Integer itrxStatus) {
        eventPublisher.publishEvent(new OrderFailedEvent(this, serial, itrxStatus));
    }
}
