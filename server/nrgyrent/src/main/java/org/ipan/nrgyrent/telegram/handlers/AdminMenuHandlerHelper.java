package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.ManagedWalletRepo;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.tron.TronTransactionHelper;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AdminMenuHandlerHelper {
    private static final long THRESHOLD = 10_000_000L;

    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final TrongridRestClient trongridRestClient;
    private final CollectionWalletRepo collectionWalletRepo;
    private final ManagedWalletRepo managedWalletRepo;
    private final ManagedWalletService managedWalletService;
    private final TronTransactionHelper tronTransactionHelper;

    @Async
    public CompletableFuture<Void> transferTrxFromCollectionWallets(Long userId, String toWallet, Long requestedAmount) {
        List<CollectionWallet> collectionWallets = collectionWalletRepo.findAllByIsActive(true);
        List<String> walletAddresses = collectionWallets.stream().map(CollectionWallet::getWalletAddress).toList();
        List<ManagedWallet> managedWallets = managedWalletRepo.findAllById(walletAddresses);

        if (managedWallets.size() != walletAddresses.size()) {
            logger.error("Not all managed wallets found for collection wallets!!!!");
            return CompletableFuture.completedFuture(null);
        }

        try {
            for (CollectionWallet collectionWallet : collectionWallets) {
                AccountInfo accountData = trongridRestClient.getAccountInfo(collectionWallet.getWalletAddress());
                Long sunBalance = accountData != null ? accountData.getBalance() : 0;
                if (sunBalance > THRESHOLD + requestedAmount) {
                    logger.info("Transferring TRX from collection wallet {} to {}", collectionWallet.getWalletAddress(), toWallet);
                    ManagedWallet managedWallet = managedWallets.stream()
                            .filter(w -> w.getBase58Address().equals(collectionWallet.getWalletAddress()))
                            .findFirst()
                            .orElse(null);
                    tronTransactionHelper.performTransferTransaction(
                            collectionWallet.getWalletAddress(),
                            toWallet,
                            requestedAmount,
                            (txId) -> managedWalletService.sign(managedWallet, txId));
                }
            }

            UserState userState = telegramState.getOrCreateUserState(userId);
            telegramMessages.sendWithdrawalSuccessful(userState);
            logger.info("TRX transfer from collection wallets completed successfully");
        } catch (Exception e) {
            logger.error("Error while transferring TRX from collection wallets", e);
            UserState userState = telegramState.getOrCreateUserState(userId);
            telegramMessages.sendWithdrawalFail(userState);
        }
        return CompletableFuture.completedFuture(null);
    }
}