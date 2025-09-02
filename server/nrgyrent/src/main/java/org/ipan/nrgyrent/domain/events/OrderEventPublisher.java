package org.ipan.nrgyrent.domain.events;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrderEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCompletedEvent(String correlationId, Integer responseStatus, String txId,
                                           String serial, Boolean isAutoDelegation, String receiveAddress,
                                           Long amount, String duration, Integer energyAmount,EnergyProviderName providerName) {
        eventPublisher.publishEvent(new OrderCompletedEvent(this, correlationId, responseStatus, txId, serial,
                providerName,
                isAutoDelegation, receiveAddress, amount, duration, energyAmount));
    }

    public void publishOrderFailedEvent(String correlationId, Integer itrxStatus, String serial) {
        eventPublisher.publishEvent(new OrderFailedEvent(this, correlationId, itrxStatus, serial));
    }

    public void publishBalanceUpdateEvent(EnergyProviderName energyProvider, Long newBalance) {
        eventPublisher.publishEvent(new BalanceUpdatedEvent(this, energyProvider, newBalance));
    }

}
