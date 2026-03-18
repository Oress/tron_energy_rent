package org.ipan.nrgyrent.netts;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory cache for the current Netts energy_1h price (in SUN per energy unit).
 */
@Component
@Slf4j
public class NettsPriceCache {
    private final AtomicReference<EnergyPrice> energy1hPrice = new AtomicReference<>();

    public void updateEnergy1hPrice(Long priceSun) {
        EnergyPrice price = new EnergyPrice(priceSun);
        energy1hPrice.set(price);
        logger.info("Updated Netts energy_1h price cache: {}", price);
    }

    public EnergyPrice getEnergy1hPrice() {
        return energy1hPrice.get();
    }

    public boolean hasEnergy1hPrice() {
        return energy1hPrice.get() != null;
    }

    @Getter
    public static class EnergyPrice {
        private final Long priceSun;

        public EnergyPrice(Long priceSun) {
            this.priceSun = priceSun;
        }

        @Override
        public String toString() {
            return "EnergyPrice{priceSun=" + priceSun + "}";
        }
    }
}
