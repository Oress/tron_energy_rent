package org.ipan.nrgyrent.domain.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderCompletedEvent extends ApplicationEvent {
    private final Integer itrxStatus;
    private final String txId;
    private final String correlationId;
    private final String serial;

    private final Boolean isAutoDelegation;
    private final String receiveAddress;
    private final String period;
    private final Long amount; // sun
    private final String duration;
    private final Integer energyAmount;

    public OrderCompletedEvent(Object eventSource, String correlationId, Integer itrxStatus, String txId,
                               String serial, Boolean isAutoDelegation, String receiveAddress,
                               String period, Long amount, String duration, Integer energyAmount) {
        super(eventSource);
        this.itrxStatus = itrxStatus;
        this.txId = txId;
        this.correlationId = correlationId;
        this.serial = serial;
        this.isAutoDelegation = isAutoDelegation;
        this.receiveAddress = receiveAddress;
        this.period = period;
        this.amount = amount;
        this.duration = duration;
        this.energyAmount = energyAmount;
    }
}
