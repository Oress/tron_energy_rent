package org.ipan.nrgyrent.cron;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
public class SweepDepositToCollectionWalletJob {
    private final CollectionWalletRepo collectionWalletRepo;
    private final BalanceRepo balanceRepo;
    private final SweepHelper sweepHelper;

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        // Get all balances from the db.
        // Split balances into batches (determined by the amount of active wallets)
        // For each deposit wallet, get the balance if balance > threshold, then deposit
        // to collection wallet
        List<CollectionWallet> collectionWallets = collectionWalletRepo.findAllByIsActive(true);
        int numOfBatches = collectionWallets.size();

        List<Balance> balances = balanceRepo.findAllByIsActive(true);
        int batchSize = balances.size() < numOfBatches 
            ? numOfBatches 
            : balances.size() / numOfBatches;
        for (int i = 0; i < balances.size(); i+= batchSize) {
            CollectionWallet collectionWallet = collectionWallets.get(i);
            List<Balance> batch = balances.subList(i * batchSize, Math.min((i + 1) * batchSize, balances.size()));

            sweepHelper.processBatch(collectionWallet, batch);
        }
    }
}
