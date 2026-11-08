package org.ipan.nrgyrent.cron;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.domain.service.UsdtExchangeSettingsService;
import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;
import java.util.List;
import org.ipan.nrgyrent.domain.model.DepositStatus;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.DepositType;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UsdtDepositOrchestrator {
    private final UsdtDepositHelper usdtDepositHelper;
    private final DepositTransactionRepo depositTransactionRepo;
    private final List<UsdtExchangeStrategy> strategies;
    private final UsdtDepositConfig usdtDepositConfig;
    private final UsdtExchangeSettingsService exchangeSettings;
    private final TelegramMessages telegramMessages;
    private final Long tgGroupId;

    public UsdtDepositOrchestrator(UsdtDepositHelper usdtDepositHelper,
                                   DepositTransactionRepo depositTransactionRepo,
                                   List<UsdtExchangeStrategy> strategies,
                                   UsdtDepositConfig usdtDepositConfig,
                                   UsdtExchangeSettingsService exchangeSettings,
                                   TelegramMessages telegramMessages,
                                   @Value("${app.notification.tggroupid}") Long tgGroupId) {
        this.usdtDepositHelper = usdtDepositHelper;
        this.depositTransactionRepo = depositTransactionRepo;
        this.strategies = strategies;
        this.usdtDepositConfig = usdtDepositConfig;
        this.exchangeSettings = exchangeSettings;
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

        if (depositTransaction.getStatus() != DepositStatus.USDT_ACKNOWLEDGED) {
            return;
        }

        // Hold new deposits when USDT topups are disabled.
        if (!usdtDepositConfig.isEnabled()) {
            logger.warn("USDT topup is disabled. Putting deposit on hold: id: {} txId: {}",
                    depositTransaction.getId(), depositTransaction.getTxId());
            depositTransaction.setStatus(DepositStatus.USDT_TOPUP_DISABLED);
            depositTransactionRepo.save(depositTransaction);
            telegramMessages.sendUsdtTopupDisabledAdmin(tgGroupId, depositTransaction);
            return;
        }

        if (depositTransactionRepo.transitionStatus(depositTransactionId, DepositStatus.USDT_ACKNOWLEDGED,
                DepositStatus.USDT_EXCHANGE_PREPARING) != 1) {
            return;
        }
        depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_PREPARING);
        depositTransaction.setExchangeProvider(exchangeSettings.currentProvider());
        depositTransactionRepo.save(depositTransaction);
        logger.info("USDT exchange preparing: depositId={}, provider={}, originalTx={}, originalMicroUsdt={}",
                depositTransactionId, depositTransaction.getExchangeProvider(), depositTransaction.getTxId(),
                depositTransaction.getOriginalAmount());
        try {
            String destination = strategy(depositTransaction).prepare(depositTransaction);
            usdtDepositHelper.tryActivateWallet(depositTransaction);
            Thread.sleep(2000);
            usdtDepositHelper.rentEnergyForUsdtTransfer(depositTransaction, destination);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_PREPARATION_FAILED);
            depositTransactionRepo.save(depositTransaction);
            logger.error("USDT exchange preparation failed: depositId={}, provider={}, exchangeOrderId={}",
                    depositTransaction.getId(), depositTransaction.getExchangeProvider(), depositTransaction.getExchangeOrderId(), e);
        }
    }

    @Async(CronJobConfig.USDT_DEPOSIT_EXECUTOR)
    public void continueOrchestrateUsdtDepositWithOrderId(String orderCorrelationId) {
        try {
            logger.debug("USDT exchange callback received; waiting for energy: energyOrderId={}", orderCorrelationId);
            // just in case depositTransaction can be locked.
            // and wait for actual energy delegation.
            Thread.sleep(50000);

            DepositTransaction depositTransaction = depositTransactionRepo.findBySystemOrderCorrelationId(orderCorrelationId);

            if (depositTransaction == null || depositTransaction.getStatus() != DepositStatus.USDT_ENERGY_RENTED) {
                logger.debug("USDT exchange callback skipped: energyOrderId={}, depositId={}, status={}",
                        orderCorrelationId, depositTransaction == null ? null : depositTransaction.getId(),
                        depositTransaction == null ? null : depositTransaction.getStatus());
                return;
            }
            logger.info("USDT exchange continuing: depositId={}, provider={}, exchangeOrderId={}, energyOrderId={}",
                    depositTransaction.getId(), depositTransaction.getExchangeProvider(),
                    depositTransaction.getExchangeOrderId(), orderCorrelationId);
            strategy(depositTransaction).exchange(depositTransaction);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.error("Error during USDT deposit orchestration for order: {}", orderCorrelationId, e);
        }
    }

    private UsdtExchangeStrategy strategy(DepositTransaction deposit) {
        // Rows created before provider selection was introduced belong to Bybit.
        UsdtExchangeProvider provider = deposit.getExchangeProvider() == null
                ? UsdtExchangeProvider.BYBIT : deposit.getExchangeProvider();
        return strategies.stream().filter(s -> s.provider() == provider).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing USDT exchange strategy: " + provider));
    }
}
