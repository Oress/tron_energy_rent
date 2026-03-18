package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.netts.NettsPriceCache;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsPricingResponse;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class NettsPriceMonitorCronJob {

    private final NettsRestClient nettsRestClient;
    private final NettsPriceCache nettsPriceCache;

    public void refreshPrices() {
        try {
            NettsPricingResponse response = nettsRestClient.getPricing();
            if (response == null || !response.isSuccess() || response.getData() == null) {
                logger.error("Failed to fetch Netts pricing: response={}", response);
                return;
            }

            NettsPricingResponse.ServicesResponse services = response.getData().getServices();
            if (services == null || services.getEnergy1h() == null) {
                logger.error("Netts pricing response missing energy_1h service");
                return;
            }

            NettsPricingResponse.EnergyServiceResponse energy1h = services.getEnergy1h();
            if (energy1h.getPeriods() == null) {
                logger.error("Netts pricing response missing energy_1h periods");
                return;
            }

            energy1h.getPeriods().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsCurrent()))
                    .findFirst()
                    .ifPresentOrElse(
                            p -> nettsPriceCache.updateEnergy1hPrice(p.getPrice()),
                            () -> logger.warn("No current period found in Netts energy_1h pricing")
                    );
        } catch (Exception e) {
            logger.error("Error fetching Netts pricing", e);
        }
    }
}
