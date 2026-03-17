package org.ipan.nrgyrent.netts;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory cache for AML provider prices (USDT and TRX).
 */
@Component
@Slf4j
public class AmlPriceCache {
    private final ConcurrentMap<AmlProvider, AmlPrice> prices = new ConcurrentHashMap<>();

    public void updatePrice(AmlProvider provider, Double priceUsdt, Double priceTrx) {
        AmlPrice price = new AmlPrice(priceUsdt, priceTrx);
        prices.put(provider, price);
        logger.info("Updated AML price cache for provider {}: {}", provider, price);
    }

    public AmlPrice getPrice(AmlProvider provider) {
        return prices.get(provider);
    }

    public boolean hasPriceFor(AmlProvider provider) {
        return prices.containsKey(provider);
    }

    @Getter
    public static class AmlPrice {
        private final Double priceUsdt;
        private final Double priceTrx;

        public AmlPrice(Double priceUsdt, Double priceTrx) {
            this.priceUsdt = priceUsdt;
            this.priceTrx = priceTrx;
        }

        @Override
        public String toString() {
            return "AmlPrice{priceUsdt=" + priceUsdt + ", priceTrx=" + priceTrx + "}";
        }
    }
}
