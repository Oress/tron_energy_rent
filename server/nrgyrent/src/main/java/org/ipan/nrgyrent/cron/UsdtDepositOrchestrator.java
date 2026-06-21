package org.ipan.nrgyrent.cron;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.DepositData;
import org.ipan.nrgyrent.domain.model.DepositStatus;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.DepositType;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UsdtDepositOrchestrator {
    private final UsdtDepositHelper usdtDepositHelper;
    private final DepositTransactionRepo depositTransactionRepo;
    private final BybitRestClient bybitRestClient;
    private final ConfigurableEnvironment configurableEnvironment;
    private final UsdtDepositConfig usdtDepositConfig;
    private final TelegramMessages telegramMessages;
    private final Long tgGroupId;

    public UsdtDepositOrchestrator(UsdtDepositHelper usdtDepositHelper,
                                   DepositTransactionRepo depositTransactionRepo,
                                   BybitRestClient bybitRestClient,
                                   ConfigurableEnvironment configurableEnvironment,
                                   UsdtDepositConfig usdtDepositConfig,
                                   TelegramMessages telegramMessages,
                                   @Value("${app.notification.tggroupid}") Long tgGroupId) {
        this.usdtDepositHelper = usdtDepositHelper;
        this.depositTransactionRepo = depositTransactionRepo;
        this.bybitRestClient = bybitRestClient;
        this.configurableEnvironment = configurableEnvironment;
        this.usdtDepositConfig = usdtDepositConfig;
        this.telegramMessages = telegramMessages;
        this.tgGroupId = tgGroupId;
    }

    @SneakyThrows
    public void startOrchestrateUsdtDeposit(Long depositTransactionId) {
        DepositTransaction depositTransaction = depositTransactionRepo.findById(depositTransactionId).get();

        if (!DepositType.USDT.equals(depositTransaction.getType())) {
            logger.error("Deposit transaction type is not USDT: {}", depositTransaction.getType());
            return;
        }

        // USDT topup disabled: record on hold, alert admins, do not start the Bybit exchange flow
        if (!usdtDepositConfig.isEnabled()) {
            logger.warn("USDT topup is disabled. Putting deposit on hold: id: {} txId: {}",
                    depositTransaction.getId(), depositTransaction.getTxId());
            depositTransaction.setStatus(DepositStatus.USDT_TOPUP_DISABLED);
            depositTransactionRepo.save(depositTransaction);
            telegramMessages.sendUsdtTopupDisabledAdmin(tgGroupId, depositTransaction);
            return;
        }

        usdtDepositHelper.tryActivateWallet(depositTransaction);
        Thread.sleep(2000);
        usdtDepositHelper.rentEnergyForUsdtTransfer(depositTransaction);
    }

    @Async(CronJobConfig.USDT_DEPOSIT_EXECUTOR)
    public void continueOrchestrateUsdtDepositWithOrderId(String orderCorrelationId) {
        try {
            // just in case depositTransaction can be locked.
            // and wait for actual energy delegation.
            Thread.sleep(50000);

            DepositTransaction depositTransaction = depositTransactionRepo.findBySystemOrderCorrelationId(orderCorrelationId);

            String txId = usdtDepositHelper.transferUsdtToBybit(depositTransaction);
            if (DepositStatus.USDT_TRANSFERRED_TO_BYBIT_FAILED.equals(depositTransaction.getStatus())) {
                logger.error("Failed to transfer USDT to Bybit for deposit transaction: {}", depositTransaction.getId());
                return;
            }

            // skip it for dev, leave it for prod, because bybit testnet does not support nile tests
            if (!configurableEnvironment.matchesProfiles("dev")) {
                for (int i = 0; i < 70; i++) {
                    DepositData depositData = bybitRestClient.getUsdtDeposits(txId);
                    // https://bybit-exchange.github.io/docs/v5/enum#depositstatus
                    if (depositData != null && depositData.getStatus() != null
                            && depositData.getConfirmationsInt() > 30 // bybit # of confirmations should be > 20
                            && (depositData.getStatus() == 10012 || depositData.getStatus() == 3)) {
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
