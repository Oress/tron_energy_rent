package org.ipan.nrgyrent.domain.events;

import lombok.Getter;
import lombok.ToString;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.springframework.context.ApplicationEvent;

@Getter
@ToString
public class BalanceUpdatedEvent extends ApplicationEvent {
    private final EnergyProviderName energyProviderName;
    private final Long newBalance;

    public BalanceUpdatedEvent(Object eventSource, EnergyProviderName energyProviderName, Long newBalance) {
        super(eventSource);
        this.energyProviderName = energyProviderName;
        this.newBalance = newBalance;
    }
}
