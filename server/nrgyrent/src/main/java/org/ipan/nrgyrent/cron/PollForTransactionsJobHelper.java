package org.ipan.nrgyrent.cron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.Contract;
import org.ipan.nrgyrent.tron.trongrid.model.ContractParameterValue;
import org.ipan.nrgyrent.tron.trongrid.model.RawData;
import org.ipan.nrgyrent.tron.trongrid.model.Transaction;
import org.ipan.nrgyrent.tron.utils.ByteArray;
import org.ipan.nrgyrent.tron.wallet.WalletApi;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
@AllArgsConstructor
public class PollForTransactionsJobHelper {
    private static final Comparator<Transaction> COMPARING_TX_TS = Comparator.<Transaction>comparingLong(tx -> tx.getRaw_data().getTimestamp());
    private static final String TRANSFER_CONTRACT = "TransferContract";

    private final DepositTransactionRepo depositTransactionRepo;
    private final TrongridRestClient trongridRestClient;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AppUserRepo appUserRepo;

    @Transactional
    @Async(CronJobConfig.TRON_TRANSACTION_EXECUTOR)
    public CompletableFuture<Void> processBatch(List<Balance> batch) {
        // TODO: watch out for OptimisticLockException

        EntityManager em = getEntityManager();

        for (Balance balance : batch) {
            String depositAddress = balance.getDepositAddress();
            logger.info("Processing balance: id: {} wallet: {}", balance.getId(), depositAddress);
            try {
                List<Transaction> data = trongridRestClient.getTransactions(depositAddress, true, true, null, 50);

                if (data != null && !data.isEmpty()) {
                    String lastTxId = balance.getLastTxId();
                    Long lastTxTimestamp = balance.getLastTxTimestamp();
                    // Use the last transaction ID and timestamp to filter out old transactions, and consider only new ones

                    logger.info("Balance Id {} wallet {} Last transaction ID: {}, Last transaction timestamp: {}", balance.getId(), depositAddress, balance.getLastTxId(), balance.getLastTxTimestamp());

                    List<Transaction> transactions = Collections.emptyList();

                    if (lastTxTimestamp != null) {
                        transactions = data
                                .stream()
                                .filter(tx -> isNewIncommingTransferContractWithMinAmount(tx, lastTxTimestamp, depositAddress))
                                .sorted(COMPARING_TX_TS)
                                .toList();
                    } else if (lastTxId != null) {
                        transactions = data
                                .stream()
                                .sorted(COMPARING_TX_TS)
                                .dropWhile(tx -> !lastTxId.equals(tx.getTxID()) && isNewIncommingTransferContractWithMinAmount(tx, 0L, depositAddress))
                                .toList();
                    } else {
                        // No last transaction ID or timestamp, consider all transactions
                        transactions = data
                                .stream()
                                .filter(tx -> isNewIncommingTransferContractWithMinAmount(tx, 0L, depositAddress))
                                .sorted(COMPARING_TX_TS)
                                .toList();
                    }

                    if (transactions.isEmpty()) {
                        logger.info("No new transactions found for address: {}", depositAddress);
                        continue;
                    }
                    logger.info("Found {} new transactions for address: {}", transactions.size(), balance.getDepositAddress());

                    // Contract is a list but only 1 element is used https://developers.tron.network/docs/tron-protocol-transaction
                    Long topUp = 0L;
                    List<DepositTransaction> depositTransactions = new ArrayList<>();

                    for (Transaction tx : transactions) {
                        logger.info("Balance Id: {} wallet: {} Transaction ID: {}, Amount: {}", balance.getId(), depositAddress, tx.getTxID(), tx.getRaw_data().getContract().getFirst().getParameter().getValue().getAmount());
                        topUp += tx.getRaw_data().getContract().getFirst().getParameter().getValue().getAmount();
                        DepositTransaction depositTransaction = createDepositTransaction(tx);
                        depositTransactions.add(depositTransaction);
                    }

                    logger.info("Top up amount for address {} {}", depositAddress, topUp);
                    balance.makeDeposit(topUp);

                    balance.setLastTxId(transactions.get(transactions.size() - 1).getTxID());
                    balance.setLastTxTimestamp(transactions.get(transactions.size() - 1).getRaw_data().getTimestamp());

                    depositTransactionRepo.saveAll(depositTransactions);
                    em.merge(balance);

                    if (BalanceType.INDIVIDUAL.equals(balance.getType())) {
                        logger.info("Sending top-up notification for individual balance ID: {}", balance.getId());
                        AppUser user = appUserRepo.findByBalanceId(balance.getId());
                        UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                        telegramMessages.sendTopupNotification(userState);
                    } else if (BalanceType.GROUP.equals(balance.getType())) {
                        logger.info("Sending top-up notification for group balance ID: {}", balance.getId());
                        Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(balance.getId());
                        for (AppUser user : users) {
                            UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                            telegramMessages.sendTopupNotification(userState);
                        }
                    } else {
                        logger.error("Unknown balance type for balance ID: {}", balance.getId());
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing balance: id: {} wallet: {}", balance.getId(), depositAddress, e);
                continue; // Skip this balance if there's an error
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private DepositTransaction createDepositTransaction(Transaction tx) {
        DepositTransaction depositTransaction = new DepositTransaction();
        ContractParameterValue value = tx.getRaw_data().getContract().getFirst().getParameter().getValue();

        byte[] fromHexString = ByteArray.fromHexString(value.getTo_address());
        String fromBase58 = WalletApi.encode58Check(fromHexString);

        byte[] toHexString = ByteArray.fromHexString(value.getOwner_address());
        String toBase58 = WalletApi.encode58Check(toHexString);

        depositTransaction.setWalletTo(fromBase58);
        depositTransaction.setWalletFrom(toBase58);
        depositTransaction.setAmount(value.getAmount());
        depositTransaction.setTxId(tx.getTxID());
        depositTransaction.setTimestamp(tx.getRaw_data().getTimestamp());

        return depositTransaction;
    }

    private boolean isNewIncommingTransferContractWithMinAmount(Transaction tx, Long lastTxTimestamp, String walletAddress) {
        RawData rawData = tx.getRaw_data();

        // basic null checks
        if (rawData == null || rawData.getContract() == null || rawData.getContract().isEmpty()) {
            logger.error("RawData or contract is null or empty for transaction: {}", tx);
            return false;
        }

        // It may be empty mind the 0.000001 transactions
        Long ts = rawData.getTimestamp();
        if (ts == null) {
            logger.warn("Timestamp is null for transaction: {} wallet {}", tx.getTxID(), walletAddress);
        }

        Contract contract = rawData.getContract().get(0);
        return TRANSFER_CONTRACT.equals(contract.getType())
                && ts != null && ts > lastTxTimestamp
                && WalletApi.encode58Check(ByteArray.fromHexString(contract.getParameter().getValue().getTo_address())).equals(walletAddress)
                && contract.getParameter().getValue().getAmount() >= AppConstants.MIN_TRANSFER_AMOUNT_SUN;
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new IllegalStateException("This method should be overridden by Spring");
    }
}
