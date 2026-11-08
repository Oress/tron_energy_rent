package org.ipan.nrgyrent.fixedfloat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatMonitor {
    static final List<DepositStatus> PENDING_STATUSES = List.of(DepositStatus.USDT_FF_TRANSFERRING,
            DepositStatus.USDT_FF_IN_PROGRESS, DepositStatus.USDT_FF_REVIEW_REQUIRED);
    private final FixedFloatClient client;
    private final DepositTransactionRepo deposits;
    private final FixedFloatSettlementService settlement;
    private final BalanceRepo balances;
    private final AppUserRepo users;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final FixedFloatReviewNotificationService reviewNotifications;
    // Logging only: settlement and retry decisions always use the database/provider response.
    private final Map<Long, String> observedStatuses = new ConcurrentHashMap<>();

    // Continue tracking funded orders across restarts, provider switches, and disabling new deposits.
    @Scheduled(fixedDelayString = "${app.fixedfloat.poll-interval-ms:30000}")
    public void poll() {
        List<DepositTransaction> pending = deposits.findByExchangeProviderAndStatusIn(
                UsdtExchangeProvider.FIXEDFLOAT, PENDING_STATUSES);
        observedStatuses.keySet().retainAll(pending.stream().map(DepositTransaction::getId).collect(Collectors.toSet()));
        logger.debug("FixedFloat reconciliation poll: pendingDeposits={}", pending.size());
        for (DepositTransaction deposit : pending) {
            try {
                FixedFloatOrder order = client.getOrder(deposit.getExchangeOrderId(), deposit.getExchangeOrderToken());
                FixedFloatUsdtExchangeStrategy.validateOrder(order, deposit);
                String previousStatus = observedStatuses.put(deposit.getId(), order.status());
                if (!Objects.equals(previousStatus, order.status())) {
                    logger.info("FixedFloat provider status: depositId={}, orderId={}, previousStatus={}, providerStatus={}, localStatus={}",
                            deposit.getId(), order.id(), previousStatus, order.status(), deposit.getStatus());
                } else {
                    logger.debug("FixedFloat provider status unchanged: depositId={}, orderId={}, providerStatus={}, localStatus={}",
                            deposit.getId(), order.id(), order.status(), deposit.getStatus());
                }
                if ("DONE".equals(order.status())) {
                    DepositTransaction completed = settlement.settle(deposit.getId(), order);
                    if (completed != null) {
                        logger.info("FixedFloat settlement committed: depositId={}, orderId={}, creditedSun={}, trxTx={}",
                                completed.getId(), completed.getExchangeOrderId(), completed.getAmount(), completed.getExchangeTrxTx());
                        observedStatuses.remove(deposit.getId());
                        notifyUsers(completed, false);
                    }
                } else if (!Set.of("NEW", "PENDING", "EXCHANGE", "WITHDRAW").contains(order.status())) {
                    boolean reviewRequired = deposit.getStatus() == DepositStatus.USDT_FF_REVIEW_REQUIRED;
                    if (!reviewRequired) {
                        reviewRequired = deposits.transitionStatus(deposit.getId(), deposit.getStatus(),
                                DepositStatus.USDT_FF_REVIEW_REQUIRED) == 1;
                        if (reviewRequired) {
                            logger.error("FixedFloat order requires review: depositId={}, orderId={}, providerStatus={}, usdtTx={}",
                                    deposit.getId(), deposit.getExchangeOrderId(), order.status(), deposit.getExchangeUsdtTx());
                        }
                    }
                    // The API does not document a distinct AML flag. Use a neutral review notice
                    // with conditional AML instructions, also for unknown future problem statuses.
                    if (reviewRequired) notifyUsers(deposit, true);
                }
            } catch (Exception e) {
                logger.error("FixedFloat reconciliation failed: depositId={}, orderId={}, localStatus={}, usdtTx={}",
                        deposit.getId(), deposit.getExchangeOrderId(), deposit.getStatus(), deposit.getExchangeUsdtTx(), e);
            }
        }
    }

    private void notifyUsers(DepositTransaction deposit, boolean requiresReview) {
        Balance balance = balances.findByDepositAddress(deposit.getWalletTo());
        if (balance == null) throw new IllegalStateException("Cannot find balance for FixedFloat notification");
        Set<AppUser> recipients;
        if (balance.getType() == BalanceType.GROUP) {
            recipients = users.findAllByGroupBalanceId(balance.getId());
        } else {
            AppUser user = users.findByBalanceId(balance.getId());
            recipients = user == null ? Set.of() : Set.of(user);
        }
        if (recipients.isEmpty()) {
            logger.warn("FixedFloat notification has no recipients: depositId={}, orderId={}, balanceId={}, requiresReview={}",
                    deposit.getId(), deposit.getExchangeOrderId(), balance.getId(), requiresReview);
        }
        for (AppUser user : recipients) {
            try {
                if (requiresReview) {
                    reviewNotifications.notifyOnce(deposit.getId(), user.getTelegramId());
                } else {
                    telegramMessages.sendTopupUsdtNotification(
                            telegramState.getOrCreateUserState(user.getTelegramId()), deposit);
                    logger.info("FixedFloat completion notice sent: depositId={}, orderId={}, telegramId={}",
                            deposit.getId(), deposit.getExchangeOrderId(), user.getTelegramId());
                }
            } catch (Exception e) {
                logger.error("FixedFloat notification failed: depositId={}, orderId={}, telegramId={}, requiresReview={}",
                        deposit.getId(), deposit.getExchangeOrderId(), user.getTelegramId(), requiresReview, e);
            }
        }
    }
}
