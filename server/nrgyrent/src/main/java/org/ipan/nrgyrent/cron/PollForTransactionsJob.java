package org.ipan.nrgyrent.cron;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.cron.tron.transaction.enabled")
public class PollForTransactionsJob {
    private static final Integer BATCH_SIZE = 10;

    private final BalanceRepo balanceRepo;
    private final PollForTransactionsJobHelper helper;

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        List<Balance> activeBalances = balanceRepo.findAllByIsActive(Boolean.TRUE);

        for (int i = 0; i < activeBalances.size(); i += BATCH_SIZE) {
            List<Balance> batch = activeBalances.subList(i, Math.min(i + BATCH_SIZE, activeBalances.size()));
            helper.processBatch(batch);
        }
    }
}
