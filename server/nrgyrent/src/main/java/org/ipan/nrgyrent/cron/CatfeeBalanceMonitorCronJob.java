package org.ipan.nrgyrent.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.catfee.CatfeeRestClient;
import org.ipan.nrgyrent.catfee.dto.CfAccountResponse;
import org.ipan.nrgyrent.catfee.dto.CfResponse;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatfeeBalanceMonitorCronJob {
    private final CatfeeRestClient restClient;
    private final OrderEventPublisher eventPublisher;

    public void checkBalance() {
        try {
            CfResponse<CfAccountResponse> account = restClient.getAccount();
            // Reuse the balance persistence and alerts triggered by CatFee orders.
            eventPublisher.publishBalanceUpdateEvent(EnergyProviderName.CATFEE, account.getData().getBalance());
        } catch (Exception e) {
            logger.error("Failed to check CATFEE balance", e);
        }
    }
}
