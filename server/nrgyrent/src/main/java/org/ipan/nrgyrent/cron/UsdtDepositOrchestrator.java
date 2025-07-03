package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.DepositData;
import org.ipan.nrgyrent.domain.model.DepositStatus;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.DepositType;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class UsdtDepositOrchestrator {
    private UsdtDepositHelper usdtDepositHelper;
    private DepositTransactionRepo depositTransactionRepo;
    private BybitRestClient bybitRestClient;
    private ConfigurableEnvironment configurableEnvironment;

    @SneakyThrows
    public void startOrchestrateUsdtDeposit(Long depositTransactionId) {
        DepositTransaction depositTransaction = depositTransactionRepo.findById(depositTransactionId).get();

        if (!DepositType.USDT.equals(depositTransaction.getType())) {
            logger.error("Deposit transaction type is not USDT: {}", depositTransaction.getType());
            return;
        }
        usdtDepositHelper.tryActivateWallet(depositTransaction);
        Thread.sleep(2000);
        usdtDepositHelper.rentEnergyForUsdtTransfer(depositTransaction);
    }

    @Async
    public void continueOrchestrateUsdtDepositWithOrderId(String orderCorrelationId) {
        try {
            DepositTransaction depositTransaction = depositTransactionRepo.findBySystemOrderCorrelationId(orderCorrelationId);

            String txId = usdtDepositHelper.transferUsdtToBybit(depositTransaction);
            if (DepositStatus.USDT_TRANSFERRED_TO_BYBIT_FAILED.equals(depositTransaction.getStatus())) {
                logger.error("Failed to transfer USDT to Bybit for deposit transaction: {}", depositTransaction.getId());
                return;
            }

            // skip it for dev, leave it for prod, because bybit testnet does not support nile tests
            if (!configurableEnvironment.matchesProfiles("dev")) {
                for (int i = 0; i < 20; i++) {
                    DepositData depositData = bybitRestClient.getUsdtDeposits(txId);
                    // https://bybit-exchange.github.io/docs/v5/enum#depositstatus
                    if (depositData != null && depositData.getStatus() != null && (depositData.getStatus() == 10012 || depositData.getStatus() == 3)) {
                        break;
                    }
                    Thread.sleep(3000);
                }
            }

            usdtDepositHelper.placeBuyOrderUsdtToTrx(depositTransaction);

            if (DepositStatus.USDT_MOVED_TO_UTA_FAILED.equals(depositTransaction.getStatus())) {
                logger.error("Bybit. Failed to move USDT from funding to UTA id: {}", depositTransaction.getId());
            }

            if (DepositStatus.USDT_MARKET_ORDER_PLACED_FAILED.equals(depositTransaction.getStatus())) {
                logger.error("Bybit. Failed to place order for deposit transaction : {}", depositTransaction.getId());
            }
        } catch (Exception e) {
            logger.error("Error during USDT deposit orchestration for order: {}", orderCorrelationId, e);
        }
    }
}
