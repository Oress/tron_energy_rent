package org.ipan.nrgyrent.application.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.TrongridConfig;
import org.ipan.nrgyrent.cron.UsdtDepositOrchestrator;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.tron.node.events.ContractTypes;
import org.ipan.nrgyrent.tron.node.events.dto.AddressTransactionEvent;
import org.ipan.nrgyrent.tron.node.events.dto.SmartContractEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class DepositService {
    private final DepositTransactionRepo depositTransactionRepo;
    private final BalanceRepo balanceRepo;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final TelegramState telegramState;
    private final TrongridConfig trongridConfig;
    private final UsdtDepositOrchestrator usdtDepositOrchestrator;

    @Async
    @Transactional
    public void processTxEventAsync(AddressTransactionEvent tx) {
        // TRANSFER TRX
        if ("trx".equals(tx.getAssetName()) && "SUCCESS".equals(tx.getResult()) && ContractTypes.TRANSFER.equals(tx.getContractType())) {
            // Check if the transaction is already processed
            DepositTransaction byTxId = depositTransactionRepo.findByTxId(tx.getTransactionId());
            if (byTxId != null && DepositStatus.COMPLETED.equals(byTxId.getStatus())) {
                logger.warn("TRX. Transaction already processed: {}", tx.getTransactionId());
                return;
            }

            if (AppConstants.MIN_TRANSFER_AMOUNT_SUN.compareTo(tx.getAssetAmount()) > 0) {
                logger.warn("TRX. The incoming amount is less than minimum deposit amount: {}", tx.getTransactionId());
                return;
            }

            String depositAddress = tx.getToAddress();
            Balance balance = balanceRepo.findByDepositAddress(depositAddress);
            logger.info("TRX. Balance Id: {} wallet: {} Transaction ID: {}, Amount: {}", balance.getId(), depositAddress, tx.getTransactionId(), tx.getAssetAmount());
            DepositTransaction depositTransaction = createDepositTransaction(tx);

            logger.info("TRX. Top up amount for address {} {}", depositAddress, tx.getAssetAmount());
            balance.makeDeposit(tx.getAssetAmount());

            balance.setLastTxId(tx.getTransactionId());
            balance.setLastTxTimestamp(tx.getTimeStamp());

            depositTransactionRepo.save(depositTransaction);
            balanceRepo.save(balance);

            if (BalanceType.INDIVIDUAL.equals(balance.getType())) {
                logger.info("Sending top-up notification for individual balance ID: {}", balance.getId());
                AppUser user = appUserRepo.findByBalanceId(balance.getId());
                UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                telegramMessages.sendTopupNotification(userState, tx.getAssetAmount());
            } else if (BalanceType.GROUP.equals(balance.getType())) {
                logger.info("Sending top-up notification for group balance ID: {}", balance.getId());
                Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(balance.getId());
                for (AppUser user : users) {
                    UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                    telegramMessages.sendTopupNotification(userState, tx.getAssetAmount());
                }
            }
        }
    }

    @Async
    @Transactional
    public void processUsdtEventAsync(SmartContractEvent tx) {
        if ("Transfer".equals(tx.getEventName()) && trongridConfig.getUsdtAddress().equals(tx.getContractAddress())) {
            String depositAddress = tx.getToAddress();
            DepositTransaction byTxId = depositTransactionRepo.findByTxId(tx.getTransactionId());
            if (byTxId != null) {
                logger.warn("USDT. Transaction already processed: {} status: {}", tx.getTransactionId(), byTxId.getStatus());
                return;
            }

            if (AppConstants.MIN_TRANSFER_AMOUNT_USDT.compareTo(tx.getAssetAmount()) > 0) {
                logger.warn("USDT. The incoming amount is less than minimum deposit amount: {}", tx.getTransactionId());
                return;
            }

            Balance balance = balanceRepo.findByDepositAddress(depositAddress);
            logger.info("USDT. Balance Id: {} wallet: {} Transaction ID: {}, Amount: {}", balance.getId(), depositAddress, tx.getTransactionId(), tx.getAssetAmount());
            DepositTransaction depositTransaction = createUsdtDepositTransaction(tx);

            logger.info("USDT. Top up amount for address {} {}", depositAddress, tx.getAssetAmount());
            balance.setLastTrc20TxId(tx.getTransactionId());
            balance.setLastTrc20TxTimestamp(tx.getTimeStamp());

            depositTransactionRepo.save(depositTransaction);
            balanceRepo.save(balance);

            usdtDepositOrchestrator.startOrchestrateUsdtDeposit(depositTransaction.getId());
        }
    }

    private DepositTransaction createDepositTransaction(AddressTransactionEvent tx) {
        DepositTransaction depositTransaction = new DepositTransaction();
        depositTransaction.setWalletTo(tx.getToAddress());
        depositTransaction.setWalletFrom(tx.getFromAddress());
        depositTransaction.setAmount(tx.getAssetAmount());
        depositTransaction.setType(DepositType.TRX);
        depositTransaction.setStatus(DepositStatus.COMPLETED);
        depositTransaction.setTxId(tx.getTransactionId());
        depositTransaction.setTimestamp(tx.getTimeStamp());

        return depositTransaction;
    }

    private DepositTransaction createUsdtDepositTransaction(SmartContractEvent tx) {
        DepositTransaction depositTransaction = new DepositTransaction();

        depositTransaction.setWalletTo(tx.getToAddress());
        depositTransaction.setWalletFrom(tx.getOriginAddress());
        depositTransaction.setOriginalAmount(tx.getAssetAmount());
        depositTransaction.setType(DepositType.USDT);
        depositTransaction.setStatus(DepositStatus.USDT_ACKNOWLEDGED);
        depositTransaction.setTxId(tx.getTransactionId());
        depositTransaction.setTimestamp(tx.getTimeStamp());

        return depositTransaction;
    }

}
