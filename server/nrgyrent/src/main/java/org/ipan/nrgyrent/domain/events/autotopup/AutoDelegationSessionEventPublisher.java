package org.ipan.nrgyrent.domain.events.autotopup;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.events.FullNodeDisconnectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class AutoDelegationSessionEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishSuccessfulDelegationEvent(Long sessionId, Long orderId) {
        eventPublisher.publishEvent(new AutoEnergyDelegatedEvent(this, sessionId, orderId));
    }

    //I don't see the reason for different event types at this moment
    // because it's only used for updating status message.
    public void publishSessionDeactivated(Long sessionId) {
        eventPublisher.publishEvent(new AutoDelegatedManuallyDeactivatedEvent(this, sessionId));
    }

    public void publishNodeReconnectEvent() {
        logger.warn("TRON NODE. Tron node has been RECONNECTED!");
    }

    public void publishNodeDisconnectedEvent() {
        logger.warn("TRON NODE. Tron node has been DISCONNECTED!");
        eventPublisher.publishEvent(new FullNodeDisconnectedEvent(this));
    }
}
