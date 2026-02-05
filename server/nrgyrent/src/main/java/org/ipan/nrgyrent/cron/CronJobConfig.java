package org.ipan.nrgyrent.cron;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class CronJobConfig {
    public static final String TRON_TRANSACTION_EXECUTOR = "tronTransactionExecutor";
    public static final String USDT_DEPOSIT_EXECUTOR = "usdt_deposit_executor";

    private final ReferralCommissionHelper referralCommissionHelper;
    private final BalanceRepo balanceRepo;
    private final CollectionWalletRepo collectionWalletRepo;
    private final CollectionWalletBalanceMonitorJob collectionWalletBalanceMonitorJob;
    private final BybitBalanceMonitorJob bybitBalanceMonitorJob;
    private final AutoDelegationMonitorJob autoDelegationMonitorJob;

    private final ItrxBalanceMonitorCronJob itrxBalanceMonitorCronJob;
    private final ItrxBalanceMonitorCronJob trxxBalanceMonitorCronJob;
    private final NettsBalanceMonitorCronJob nettsBalanceMonitorCronJob;

    private final AutoDelegationLowBalanceMonitorJob autoDelegationLowBalanceMonitorJob;

    public CronJobConfig(ReferralCommissionHelper referralCommissionHelper,
                         BalanceRepo balanceRepo,
                         CollectionWalletRepo collectionWalletRepo,
                         CollectionWalletBalanceMonitorJob collectionWalletBalanceMonitorJob,
                         BybitBalanceMonitorJob bybitBalanceMonitorJob,
                         AutoDelegationMonitorJob autoDelegationMonitorJob,
                         ItrxBalanceMonitorCronJob itrxBalanceMonitorCronJob,
                         NettsBalanceMonitorCronJob nettsBalanceMonitorCronJob,
                         AutoDelegationLowBalanceMonitorJob autoDelegationLowBalanceMonitorJob,
                         @Qualifier(AppConstants.TRXX_MONITOR_JOB) ItrxBalanceMonitorCronJob trxxBalanceMonitorCronJob) {
        this.referralCommissionHelper = referralCommissionHelper;
        this.balanceRepo = balanceRepo;
        this.collectionWalletRepo = collectionWalletRepo;
        this.collectionWalletBalanceMonitorJob = collectionWalletBalanceMonitorJob;
        this.bybitBalanceMonitorJob = bybitBalanceMonitorJob;
        this.autoDelegationMonitorJob = autoDelegationMonitorJob;
        this.itrxBalanceMonitorCronJob = itrxBalanceMonitorCronJob;
        this.trxxBalanceMonitorCronJob = trxxBalanceMonitorCronJob;
        this.nettsBalanceMonitorCronJob = nettsBalanceMonitorCronJob;
        this.autoDelegationLowBalanceMonitorJob = autoDelegationLowBalanceMonitorJob;
    }

    @Bean(name = TRON_TRANSACTION_EXECUTOR)
    @ConditionalOnProperty(name = "app.cron.tron.transaction.enabled")
    public Executor asyncExecutor() {
        return Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("tronTransactionExecutor-" + thread.threadId());
            return thread;
        });
    }

    @Bean(name = USDT_DEPOSIT_EXECUTOR)
    public Executor usdtDepositExecutor() {
        return Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("usdt_deposit_executor-" + thread.threadId());
            return thread;
        });
    }

//    @Scheduled(cron = "0 0 1 * * *")
     @Scheduled(initialDelay = 1, fixedRate = 20, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        Set<Balance> pendingBalances = balanceRepo.findAllWithPendingReferralCommissions();

        for (Balance balance : pendingBalances) {
            referralCommissionHelper.processBalanceWithPendingCommissions(balance.getId());
        }
    }

    @Scheduled(initialDelay = 1, fixedRate = 3, timeUnit = TimeUnit.MINUTES)
    public void monitorCollectionWalletBalances() {
        List<CollectionWallet> all = collectionWalletRepo.findAll();

        for (CollectionWallet collectionWallet : all) {
            collectionWalletBalanceMonitorJob.processWallet(collectionWallet.getId());
        }
    }

    @Scheduled(initialDelay = 1, fixedRate = 3, timeUnit = TimeUnit.MINUTES)
    public void monitorAutoDelegationLowBalance() {
        autoDelegationLowBalanceMonitorJob.monitorFowLowBalance();
    }

    @Scheduled(fixedDelayString = "${app.alerts.itrx-balance.interval:180}", timeUnit = TimeUnit.SECONDS)
    public void monitorItrxBalance() {
        itrxBalanceMonitorCronJob.checkBalance();
    }

    @Scheduled(fixedDelayString = "${app.alerts.itrx-balance.interval:180}", timeUnit = TimeUnit.SECONDS)
    public void monitorTrxxBalance() {
        trxxBalanceMonitorCronJob.checkBalance();
    }

    @Scheduled(fixedDelayString = "${app.alerts.itrx-balance.interval:180}", timeUnit = TimeUnit.SECONDS)
    public void monitorNettsBalance() {
        nettsBalanceMonitorCronJob.checkBalance();
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void autoDelegationMonitorJob() {
        autoDelegationMonitorJob.monitorForInactiveSessions();
    }

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void monitorBybitBalance() {
        bybitBalanceMonitorJob.monitor();
    }
}
