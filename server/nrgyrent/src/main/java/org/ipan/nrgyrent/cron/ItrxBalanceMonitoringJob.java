package org.ipan.nrgyrent.cron;

import org.ipan.nrgyrent.domain.model.ItrxApiStats;
import org.ipan.nrgyrent.domain.model.repository.ItrxApiStatsRepo;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Configuration
@AllArgsConstructor
@Slf4j
public class ItrxBalanceMonitoringJob {
    private final RestClient itrxRestClient;
    private final ItrxApiStatsRepo itrxApiStatsRepo;

    // @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        logger.info("Job. Fetching API stats...");
        ApiUsageResponse apiStats = itrxRestClient.getApiStats();
        logger.info("API stats: {}", apiStats);
        saveItrxApiStats(apiStats);
    }

    private ItrxApiStats saveItrxApiStats(ApiUsageResponse apiStats) {
        ItrxApiStats stats = new ItrxApiStats();
        stats.setBalanceSun(apiStats.getBalance());
        stats.setTotalOrdersCount(apiStats.getTotal_count());
        stats.setTotalSumEnergy(apiStats.getTotal_sum_energy());
        stats.setTotalSumTrx(apiStats.getTotal_sum_trx());
        return itrxApiStatsRepo.save(stats);
    }
}
