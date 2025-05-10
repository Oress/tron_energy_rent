package org.ipan.nrgyrent.domain.events;

import lombok.Getter;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.context.ApplicationEvent;

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