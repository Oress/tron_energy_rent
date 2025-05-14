package org.ipan.nrgyrent.cron;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.Contract;
import org.ipan.nrgyrent.trongrid.model.ContractParameterValue;
import org.ipan.nrgyrent.trongrid.model.RawData;
import org.ipan.nrgyrent.trongrid.model.Transaction;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressTransactionsGet200Response;
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
    private static final Comparator<Transaction> COMPARING_TX_TS = Comparator.<Transaction>comparingLong(tx -> tx.getRawData().getTimestamp());
    private static final String TRANSFER_CONTRACT = "TransferContract";
    private static final Long MIN_TRANSFER_AMOUNT_SUN = 1_000_000L;

    private final AccountApi accountApi;

    @Transactional
    @Async(CronJobConfig.TRON_TRANSACTION_EXECUTOR)
    public void processBatch(List<Balance> batch) {
        // TODO: watch out for OptimisticLockException

        EntityManager em = getEntityManager();

        for (Balance balance : batch) {
            logger.info("Processing balance: id: {} wallet: {}", balance.getId(), balance.getDepositAddress());
            V1AccountsAddressTransactionsGet200Response response = accountApi
                    .v1AccountsAddressTransactionsGet(balance.getDepositAddress())
                    .block();

            if (response != null && response.getData() != null) {
                String lastTxId = balance.getLastTxId();
                Long lastTxTimestamp = balance.getLastTxTimestamp();
                // Use the last transaction ID and timestamp to filter out old transactions, and consider only new ones

                logger.info("Balance Id {} wallet {} Last transaction ID: {}, Last transaction timestamp: {}", balance.getId(), balance.getDepositAddress(), balance.getLastTxId(), balance.getLastTxTimestamp());

                List<Transaction> transactions = Collections.emptyList();

                if (lastTxTimestamp != null) {
                    transactions = response.getData()
                            .stream()
                            .filter(tx -> isNewTransferContractWithMinAmount(tx, lastTxTimestamp))
                            .sorted(COMPARING_TX_TS)
                            .toList();
                } else if (lastTxId != null) {
                    transactions = response.getData()
                            .stream()
                            .sorted(COMPARING_TX_TS)
                            .dropWhile(tx -> !lastTxId.equals(tx.getTxID()) && isNewTransferContractWithMinAmount(tx, 0L))
                            .toList();
                } else {
                    // No last transaction ID or timestamp, consider all transactions
                    transactions = response.getData()
                            .stream()
                            .filter(tx -> isNewTransferContractWithMinAmount(tx, 0L))
                            .sorted(COMPARING_TX_TS)
                            .toList();
                }

                if (transactions.isEmpty()) {
                    logger.info("No new transactions found for address: {}", balance.getDepositAddress());
                    continue;
                }
                logger.info("Found {} new transactions for address: {}", transactions.size(), balance.getDepositAddress());

                // Contract is a list but only 1 element is used https://developers.tron.network/docs/tron-protocol-transaction
                Long topUp = transactions
                        .stream()
                        .peek(tx -> logger.info("Balance Id: {} wallet: {} Transaction ID: {}, Amount: {}", balance.getId(), balance.getDepositAddress(), tx.getTxID(), tx.getRawData().getContract().getFirst().getParameter().getValue().getAmount()))
                        .mapToLong(tx -> tx.getRawData().getContract().getFirst().getParameter().getValue().getAmount())
                        .sum();

                logger.info("Top up amount for address {} {}", balance.getDepositAddress(), topUp);
                balance.makeDeposit(topUp);

                balance.setLastTxId(transactions.get(transactions.size() - 1).getTxID());
                balance.setLastTxTimestamp(transactions.get(transactions.size() - 1).getRawData().getTimestamp());

                DepositTransaction depositTransaction = createDepositTransaction(transactions.get(transactions.size() - 1));
                em.persist(depositTransaction);
                em.merge(balance);
            }
        }
    }

    private DepositTransaction createDepositTransaction( Transaction tx) {
        DepositTransaction depositTransaction = new DepositTransaction();
        ContractParameterValue value = tx.getRawData().getContract().getFirst().getParameter().getValue();

        depositTransaction.setWalletTo(value.getToAddress());
        depositTransaction.setWalletFrom(value.getOwnerAddress());
        depositTransaction.setAmount(value.getAmount());
        depositTransaction.setTxId(tx.getTxID());
        depositTransaction.setTimestamp(tx.getRawData().getTimestamp());

        return depositTransaction;
    }

    private boolean isNewTransferContractWithMinAmount(Transaction tx, Long lastTxTimestamp) {
        RawData rawData = tx.getRawData();

        // basic null checks
        if (rawData == null || rawData.getContract() == null || rawData.getContract().isEmpty()) {
            logger.error("RawData or contract is null or empty for transaction: {}", tx);
            return false;
        }

        Contract contract = rawData.getContract().get(0);
        return TRANSFER_CONTRACT.equals(contract.getType())
                && rawData.getTimestamp() > lastTxTimestamp
                && contract.getParameter().getValue().getAmount() >= MIN_TRANSFER_AMOUNT_SUN;
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new IllegalStateException("This method should be overridden by Spring");
    }
}
