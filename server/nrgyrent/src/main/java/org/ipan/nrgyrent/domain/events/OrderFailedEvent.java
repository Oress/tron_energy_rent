package org.ipan.nrgyrent.domain.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class OrderFailedEvent extends ApplicationEvent {
    private final Integer itrxStatus;
    private final String serial;

    public OrderFailedEvent(Object source, String serial, Integer itrxStatus) {
        super(source);
        this.itrxStatus = itrxStatus;
        this.serial = serial;
    }
}