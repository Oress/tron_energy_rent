package org.ipan.nrgyrent.cron;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.repository.ManagedWalletRepo;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
public class SweepHelper {
    private final Long SUN_THREADSHOLD = 10_000_000L;

    private final ManagedWalletRepo managedWalletRepo;
    private final ManagedWalletService managedWalletService;
    private final TrongridRestClient trongridRestClient;

    @Transactional
    // @Async
    public void processBatch(CollectionWallet collectionWallet, List<Balance> batch) {
        List<String> list = batch.stream().map(Balance::getDepositAddress).toList();
        List<ManagedWallet> allById = managedWalletRepo.findAllById(list);
        Map<String, ManagedWallet> managedWallet = allById.stream().collect(Collectors.toMap(ManagedWallet::getBase58Address, Function.identity()));
        if (managedWallet == null) {
            return;
        }

        for (Balance balance : batch) {
            AccountInfo data = trongridRestClient.getAccountInfo(balance.getDepositAddress());
           Long sunBalance = data != null ? data.getBalance() : 0;
           if (sunBalance > SUN_THREADSHOLD) {
                Long amountToTransfer = sunBalance - SUN_THREADSHOLD;
                ManagedWallet wallet = managedWallet.get(balance.getDepositAddress());

                TreeMap<String, Object> responseProps = trongridRestClient.createTransaction(
                    balance.getDepositAddress(),
                    collectionWallet.getWalletAddress(),
                    amountToTransfer
                );

                String txId = (String)responseProps.get("txID");
                String signature = managedWalletService.sign(wallet, txId);

                responseProps.put("signature", List.of(signature));
                TreeMap<String, Object> broadcastResult = trongridRestClient.broadcastTransaction(responseProps);
                String code = (String) broadcastResult.get("code");
                if (code == null || code.isEmpty()) {
                    logger.info("Transaction successful: {}", broadcastResult.get("txid"));
                } else {
                    logger.error("Transaction failed: {}", broadcastResult.get("message"));
                }
           }
        }

    }
}
