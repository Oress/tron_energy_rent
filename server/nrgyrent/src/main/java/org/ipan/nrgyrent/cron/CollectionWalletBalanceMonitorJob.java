package org.ipan.nrgyrent.cron;

import java.time.Instant;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.context.annotation.Configuration;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@AllArgsConstructor
public class CollectionWalletBalanceMonitorJob {
    private final CollectionWalletRepo collectionWalletRepo;
    private final TrongridRestClient trongridRestClient;

    @Transactional
    public void processWallet(Long walletId) {
        CollectionWallet collectionWallet = collectionWalletRepo.findById(walletId).orElse(null);
        
        if (collectionWallet != null && collectionWallet.getIsActive()) {
            AccountInfo data = trongridRestClient.getAccountInfo(collectionWallet.getWalletAddress());
            Long sunBalance = data != null ? data.getBalance() : 0;

            collectionWallet.setBalanceOnChain(sunBalance);
            collectionWallet.setBalanceLastChecked(Instant.now());

            // Rounding operation and preventing operations with dust(everything less than 0.01 TRX);
            logger.info("Monitoring coll. wallet current balance of {} is {}", collectionWallet.getWalletAddress(), sunBalance);
        } else {
            logger.error("Cannot find collection wallet for update or it's incative id {}", walletId);
        }
        
    }
}
