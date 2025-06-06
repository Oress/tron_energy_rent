package org.ipan.nrgyrent.cron;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class CronJobConfig {
    public static final String TRON_TRANSACTION_EXECUTOR = "tronTransactionExecutor";

    private final ReferralCommissionHelper referralCommissionHelper;
    private final BalanceRepo balanceRepo;

    @Bean(name = TRON_TRANSACTION_EXECUTOR)
    @ConditionalOnProperty(name = "app.cron.tron.transaction.enabled")
    public Executor asyncExecutor() {
        return Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("tronTransactionExecutor-" + thread.threadId());
            return thread;
        });
    }

    @Scheduled(initialDelay = 1, fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        balanceRepo.findAllWithPendingReferralCommissions();
        Set<Balance> pendingBalances = balanceRepo.findAllWithPendingReferralCommissions();

        for (Balance balance : pendingBalances) {
            referralCommissionHelper.processBalanceWithPendingCommissions(balance.getId());
        }
    }
}
