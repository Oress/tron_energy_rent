package org.ipan.nrgyrent;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class EnergyProviders {
    private Map<String, EnergyProvider> providers;

    public EnergyProvider getProvider(String providerName) {
        EnergyProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown energy provider: " + providerName);
        }
        return provider;
    }

}
