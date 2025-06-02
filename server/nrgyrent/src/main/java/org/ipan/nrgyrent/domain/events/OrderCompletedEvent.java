package org.ipan.nrgyrent.domain.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCompletedEvent extends ApplicationEvent {
    private final Integer itrxStatus;
    private final String txId;
    private final String correlationId;
    private final String serial;

    public OrderCompletedEvent(Object source, String correlationId, Integer itrxStatus, String txId, String serial) {
        super(source);
        this.itrxStatus = itrxStatus;
        this.txId = txId;
        this.correlationId = correlationId;
        this.serial = serial;
    }
}
