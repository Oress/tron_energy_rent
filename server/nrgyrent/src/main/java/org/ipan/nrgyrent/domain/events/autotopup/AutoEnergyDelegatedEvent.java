package org.ipan.nrgyrent.domain.events.autotopup;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class AutoEnergyDelegatedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long sessionId;

    public AutoEnergyDelegatedEvent(Object source, Long sessionId, Long orderId) {
        super(source);
        this.orderId = orderId;
        this.sessionId = sessionId;
    }
}
