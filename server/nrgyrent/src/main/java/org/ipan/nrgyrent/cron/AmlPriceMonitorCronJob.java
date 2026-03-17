package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.ipan.nrgyrent.netts.AmlPriceCache;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlPriceResponse;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class AmlPriceMonitorCronJob {

    private final NettsRestClient nettsRestClient;
    private final AmlPriceCache amlPriceCache;

    public void refreshPrices() {
        for (AmlProvider provider : AmlProvider.values()) {
            try {
                NettsAmlPriceResponse response = nettsRestClient.getAmlPrice(provider);
                if (response != null && response.isSuccess() && response.getData() != null) {
                    NettsAmlPriceResponse.DataResponse data = response.getData();
                    amlPriceCache.updatePrice(provider, data.getPriceUsdt(), data.getPriceTrx());
                } else {
                    logger.warn("Failed to fetch AML price for provider {}: response={}", provider, response);
                }
            } catch (Exception e) {
                logger.error("Error fetching AML price for provider {}", provider, e);
            }
        }
    }
}
