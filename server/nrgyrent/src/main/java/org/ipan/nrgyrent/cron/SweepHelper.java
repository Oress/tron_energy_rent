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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
public class SweepHelper {
    private final Long SUN_THREADSHOLD = 10_000_000L;

    private final Long minWithdrawValue;
    private final ManagedWalletRepo managedWalletRepo;
    private final ManagedWalletService managedWalletService;
    private final TrongridRestClient trongridRestClient;

    public SweepHelper(
        @Value("${app.sweeping.min-amount-sun:10000}") Long minAmountSun,
        ManagedWalletRepo managedWalletRepo,
        ManagedWalletService managedWalletService,
        TrongridRestClient trongridRestClient) {
        this.managedWalletRepo = managedWalletRepo;
        this.managedWalletService = managedWalletService;
        this.trongridRestClient = trongridRestClient;
        this.minWithdrawValue = minAmountSun;
    }

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

                // Rounding operation and preventing operations with dust(everything less than 0.01 TRX);
                amountToTransfer = (amountToTransfer / minWithdrawValue) * minWithdrawValue;
                if (amountToTransfer < minWithdrawValue) {
                    logger.warn(
                            "SKIP Sweeping DUST. balance is greater than threshold {} but amountToTransfer is less than min withdrawal minval {}, balance: {}",
                            SUN_THREADSHOLD, minWithdrawValue, sunBalance);
                    continue;
                }

                logger.info("Sweeping {} sun from {} to coll. wallet {}", amountToTransfer, balance.getDepositAddress(), collectionWallet.getWalletAddress());

                ManagedWallet wallet = managedWallet.get(balance.getDepositAddress());

                TreeMap<String, Object> responseProps = trongridRestClient.createTransaction(
                        balance.getDepositAddress(),
                        collectionWallet.getWalletAddress(),
                        amountToTransfer);

                String txId = (String) responseProps.get("txID");
                String signature = managedWalletService.sign(wallet, txId);

                responseProps.put("signature", List.of(signature));
                TreeMap<String, Object> broadcastResult = trongridRestClient.broadcastTransaction(responseProps);
                String code = (String) broadcastResult.get("code");
                if (code == null || code.isEmpty()) {
                    logger.info("Sweeping Successful {} sun from {} to coll. wallet {} txid: {}", amountToTransfer, balance.getDepositAddress(), collectionWallet.getWalletAddress(), broadcastResult.get("txid"));
                } else {
                    logger.error("Sweeping Successful {} sun from {} to coll. wallet {} error: {}", amountToTransfer, balance.getDepositAddress(), collectionWallet.getWalletAddress(), broadcastResult.get("message"));
                }
            }
        }
    }
}
