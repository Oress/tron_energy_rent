package org.ipan.nrgyrent.fixedfloat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedFloatSettlementService {
    private final DepositTransactionRepo deposits;
    private final BalanceRepo balances;

    @Transactional
    public DepositTransaction settle(Long depositId, FixedFloatOrder order) {
        DepositTransaction deposit = deposits.findLockedById(depositId);
        if (deposit == null || deposit.getStatus() == DepositStatus.COMPLETED) {
            logger.debug("FixedFloat settlement skipped: depositId={}, status={}",
                    depositId, deposit == null ? null : deposit.getStatus());
            return null;
        }
        if (deposit.getExchangeProvider() != UsdtExchangeProvider.FIXEDFLOAT
                || !FixedFloatMonitor.PENDING_STATUSES.contains(deposit.getStatus())) {
            throw new IllegalStateException("Deposit is not awaiting a FixedFloat payout");
        }
        FixedFloatUsdtExchangeStrategy.validateOrder(order, deposit);
        if (!"DONE".equals(order.status()) || order.from().tx() == null || order.to().tx() == null
                || deposit.getExchangeUsdtTx() == null
                || !deposit.getExchangeUsdtTx().equals(order.from().tx().id())
                || order.from().tx().amount() == null
                || order.from().tx().amount().compareTo(BigDecimal.valueOf(deposit.getExchangeSendAmount(), 6)) != 0
                || order.to().tx().id() == null || order.to().tx().id().isBlank()
                || order.to().tx().amount() == null || order.to().tx().amount().signum() <= 0) {
            logger.warn("FixedFloat payout validation failed: depositId={}, orderId={}, providerStatus={}, "
                            + "expectedUsdtTx={}, receivedUsdtTx={}, expectedMicroUsdt={}, receivedUsdt={}, trxTx={}, actualTrx={}",
                    depositId, order.id(), order.status(), deposit.getExchangeUsdtTx(),
                    order.from().tx() == null ? null : order.from().tx().id(), deposit.getExchangeSendAmount(),
                    order.from().tx() == null ? null : order.from().tx().amount(),
                    order.to().tx() == null ? null : order.to().tx().id(),
                    order.to().tx() == null ? null : order.to().tx().amount());
            throw new IllegalStateException("FixedFloat payout is not complete or does not match the transfer");
        }
        // Use the actual payout, never the quote. Provider/network fees are already deducted.
        // Value the bot fee at this order's effective rate, leaving its TRX in the collection wallet.
        BigDecimal payout = order.to().tx().amount();
        long originalAmount = deposit.getOriginalAmount();
        long sendAmount = deposit.getExchangeSendAmount();
        long customerAmount = Math.subtractExact(originalAmount,
                UsdtDepositConfig.BOT_FEE_USDT.movePointRight(6).longValueExact());
        // Orders prepared by the previous strategy already retained the fee in USDT.
        // Their persisted input amount prevents charging it a second time after an upgrade.
        if (customerAmount <= 0 || (sendAmount != originalAmount && sendAmount != customerAmount)) {
            logger.warn("FixedFloat fee validation failed: depositId={}, orderId={}, originalMicroUsdt={}, "
                            + "sentMicroUsdt={}, customerMicroUsdt={}, botFeeUsdt={}",
                    depositId, order.id(), originalAmount, sendAmount, customerAmount, UsdtDepositConfig.BOT_FEE_USDT);
            throw new IllegalStateException("FixedFloat input does not match the deposit fee calculation");
        }
        long sun = payout.movePointRight(6).multiply(BigDecimal.valueOf(customerAmount))
                .divide(BigDecimal.valueOf(sendAmount), 0, RoundingMode.DOWN).longValueExact();
        if (sun <= 0) throw new IllegalStateException("FixedFloat customer credit rounds to zero");
        if (balances.creditDeposit(deposit.getWalletTo(), sun) != 1) {
            throw new IllegalStateException("Cannot find balance for FixedFloat payout");
        }
        deposit.setAmount(sun);
        deposit.setTrxToUsdtRate(BigDecimal.valueOf(sendAmount, 6)
                .divide(payout, 6, RoundingMode.HALF_UP));
        deposit.setExchangeTrxTx(order.to().tx().id());
        deposit.setStatus(DepositStatus.COMPLETED);
        DepositTransaction saved = deposits.save(deposit);
        // This transaction has not committed yet; the monitor logs completion after the service returns.
        logger.info("FixedFloat settlement prepared: depositId={}, orderId={}, sentUsdt={}, actualPayoutTrx={}, "
                        + "customerCreditTrx={}, retainedTrx={}, effectiveUsdtPerTrx={}, feePreviouslyRetainedInUsdt={}, trxTx={}",
                depositId, order.id(), BigDecimal.valueOf(sendAmount, 6), payout, BigDecimal.valueOf(sun, 6),
                payout.subtract(BigDecimal.valueOf(sun, 6)), deposit.getTrxToUsdtRate(),
                sendAmount != originalAmount, deposit.getExchangeTrxTx());
        return saved;
    }
}
