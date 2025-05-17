package org.ipan.nrgyrent.domain.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class OrderFailedEvent extends ApplicationEvent {
    private final Integer itrxStatus;
    private final String correlationId;

    public OrderFailedEvent(Object source, String correlationId, Integer itrxStatus) {
        super(source);
        this.itrxStatus = itrxStatus;
        this.correlationId = correlationId;
    }
}