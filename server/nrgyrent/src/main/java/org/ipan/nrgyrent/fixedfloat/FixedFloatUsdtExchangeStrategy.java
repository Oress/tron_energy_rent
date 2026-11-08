package org.ipan.nrgyrent.fixedfloat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.TrongridConfig;
import org.ipan.nrgyrent.cron.UsdtExchangeStrategy;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedFloatUsdtExchangeStrategy implements UsdtExchangeStrategy {
    private final FixedFloatClient client;
    private final DepositTransactionRepo deposits;
    private final CollectionWalletRepo collectionWallets;
    private final ManagedWalletRepo wallets;
    private final ManagedWalletService walletService;
    private final TrongridRestClient trongrid;
    private final TrongridConfig tronConfig;

    @Override
    public UsdtExchangeProvider provider() {
        return UsdtExchangeProvider.FIXEDFLOAT;
    }

    @Override
    public String prepare(DepositTransaction deposit) {
        if (!"TR7NHqjeKQxGTCi8q8ZY4pL8otgjLj6t".equals(tronConfig.getUsdtAddress())) {
            throw new IllegalStateException("FixedFloat requires TRON mainnet USDT");
        }
        if (deposit.getExchangeOrderId() != null) {
            throw new IllegalStateException("FixedFloat order already exists for deposit " + deposit.getId());
        }
        CollectionWallet collection = collectionWallets.findFirstByIsActive(true);
        if (collection == null || collection.getWalletAddress() == null || collection.getWalletAddress().length() != 34
                || !WalletTools.isValidTronAddress(collection.getWalletAddress())
                || collection.getWalletAddress().equals(deposit.getWalletTo())) {
            throw new IllegalStateException("An active collection wallet is required for FixedFloat payouts");
        }
        // Convert the whole deposit. Collect the bot fee in TRX when crediting the customer,
        // so neither the fee nor an extra cent is left behind in the managed USDT wallet.
        long amount = deposit.getOriginalAmount();
        if (amount <= UsdtDepositConfig.BOT_FEE_USDT.movePointRight(6).longValueExact()) {
            throw new IllegalArgumentException("Deposit does not cover the bot fee");
        }
        try {
            logger.info("FixedFloat creating order: depositId={}, originalTx={}, sendUsdt={}, payoutAddress={}",
                    deposit.getId(), deposit.getTxId(), BigDecimal.valueOf(amount, 6), collection.getWalletAddress());
            FixedFloatOrder order = client.createOrder(BigDecimal.valueOf(amount, 6), collection.getWalletAddress());
            // Persist access details even if the returned order needs manual investigation.
            deposit.setExchangeOrderId(order.id());
            deposit.setExchangeOrderToken(order.token());
            deposit.setExchangeSendAmount(amount);
            deposit.setExchangePayoutAddress(collection.getWalletAddress());
            deposits.save(deposit);
            logger.info("FixedFloat order recorded: depositId={}, orderId={}, providerStatus={}",
                    deposit.getId(), order.id(), order.status());
            validateOrder(order, deposit);
            if (!"NEW".equals(order.status()) || order.token() == null || order.token().isBlank()
                    || order.from().address() == null || order.from().address().length() != 34
                    || !WalletTools.isValidTronAddress(order.from().address())
                    || order.from().amount() == null
                    || order.from().amount().compareTo(BigDecimal.valueOf(amount, 6)) != 0) {
                logger.warn("FixedFloat invalid funding instructions: depositId={}, orderId={}, providerStatus={}, "
                                + "expectedUsdt={}, instructedUsdt={}, depositAddress={}, hasToken={}",
                        deposit.getId(), order.id(), order.status(), BigDecimal.valueOf(amount, 6),
                        order.from().amount(), order.from().address(), order.token() != null && !order.token().isBlank());
                throw new IllegalStateException("Invalid FixedFloat deposit instructions");
            }
            deposit.setExchangeDepositAddress(order.from().address());
            deposits.save(deposit);
            logger.info("FixedFloat order ready: depositId={}, orderId={}, sendUsdt={}, depositAddress={}, quotedTrx={}",
                    deposit.getId(), order.id(), BigDecimal.valueOf(amount, 6), order.from().address(), order.to().amount());
            return order.from().address();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not create FixedFloat order", e);
        }
    }

    @Override
    public void exchange(DepositTransaction deposit) {
        // Commit the claim before any external transfer; duplicate callbacks must not send again.
        if (deposits.transitionStatus(deposit.getId(), DepositStatus.USDT_ENERGY_RENTED,
                DepositStatus.USDT_FF_TRANSFERRING) != 1) {
            logger.debug("FixedFloat transfer claim skipped: depositId={}, orderId={}, snapshotStatus={}",
                    deposit.getId(), deposit.getExchangeOrderId(), deposit.getStatus());
            return;
        }
        deposit.setStatus(DepositStatus.USDT_FF_TRANSFERRING);
        String stage = "validate-order";
        logger.info("FixedFloat transfer claimed: depositId={}, orderId={}, sendMicroUsdt={}, fromAddress={}, toAddress={}",
                deposit.getId(), deposit.getExchangeOrderId(), deposit.getExchangeSendAmount(),
                deposit.getWalletTo(), deposit.getExchangeDepositAddress());
        try {
            if (!"TR7NHqjeKQxGTCi8q8ZY4pL8otgjLj6t".equals(tronConfig.getUsdtAddress())) {
                throw new IllegalStateException("FixedFloat requires TRON mainnet USDT");
            }
            FixedFloatOrder order = client.getOrder(deposit.getExchangeOrderId(), deposit.getExchangeOrderToken());
            validateOrder(order, deposit);
            if (!"NEW".equals(order.status())
                    || !deposit.getExchangeDepositAddress().equals(order.from().address())
                    || order.from().amount() == null
                    || order.from().amount().compareTo(BigDecimal.valueOf(deposit.getExchangeSendAmount(), 6)) != 0) {
                logger.warn("FixedFloat order cannot be funded: depositId={}, orderId={}, providerStatus={}, "
                                + "expectedAddress={}, actualAddress={}, expectedMicroUsdt={}, instructedUsdt={}",
                        deposit.getId(), order.id(), order.status(), deposit.getExchangeDepositAddress(),
                        order.from().address(), deposit.getExchangeSendAmount(), order.from().amount());
                throw new IllegalStateException("FixedFloat order is no longer ready to receive the transfer");
            }
            stage = "build-transfer";
            TreeMap<String, Object> response = trongrid.transferUsdtSmartContract(deposit.getWalletTo(),
                    deposit.getExchangeDepositAddress(), deposit.getExchangeSendAmount());
            if (!(response.get("transaction") instanceof Map<?, ?> raw)
                    || !(raw.get("txID") instanceof String txId) || txId.isBlank()) {
                throw new IllegalStateException("Could not build the FixedFloat USDT transfer");
            }
            Map<String, Object> transaction = new TreeMap<>();
            raw.forEach((key, value) -> transaction.put((String) key, value));
            stage = "sign-transfer";
            ManagedWallet wallet = wallets.findById(deposit.getWalletTo()).orElseThrow();
            transaction.put("signature", List.of(walletService.sign(wallet, txId)));
            deposit.setExchangeUsdtTx(txId);
            // Record the transaction before broadcast, so an ambiguous response can be reconciled.
            stage = "persist-transfer";
            deposits.saveAndFlush(deposit);
            stage = "broadcast";
            logger.info("FixedFloat broadcasting recorded transfer: depositId={}, orderId={}, usdtTx={}",
                    deposit.getId(), deposit.getExchangeOrderId(), txId);
            TreeMap<String, Object> broadcast = trongrid.broadcastTransaction(transaction);
            if (!Boolean.TRUE.equals(broadcast.get("result")) || !txId.equals(broadcast.get("txid"))) {
                logger.warn("FixedFloat broadcast not confirmed: depositId={}, orderId={}, expectedTx={}, "
                                + "returnedTx={}, result={}, code={}",
                        deposit.getId(), deposit.getExchangeOrderId(), txId,
                        broadcast.get("txid"), broadcast.get("result"), broadcast.get("code"));
                throw new IllegalStateException("FixedFloat USDT broadcast needs reconciliation");
            }
            stage = "record-progress";
            int updated = deposits.transitionStatus(deposit.getId(), DepositStatus.USDT_FF_TRANSFERRING,
                    DepositStatus.USDT_FF_IN_PROGRESS);
            logger.info("FixedFloat broadcast accepted: depositId={}, orderId={}, usdtTx={}, progressUpdated={}",
                    deposit.getId(), deposit.getExchangeOrderId(), txId, updated == 1);
        } catch (Exception e) {
            // Never automatically resend after an ambiguous network/broadcast outcome.
            logger.error("FixedFloat transfer failed: depositId={}, orderId={}, stage={}, usdtTx={}; manual review required",
                    deposit.getId(), deposit.getExchangeOrderId(), stage, deposit.getExchangeUsdtTx(), e);
            int updated = deposits.transitionStatus(deposit.getId(), DepositStatus.USDT_FF_TRANSFERRING,
                    DepositStatus.USDT_FF_REVIEW_REQUIRED);
            logger.warn("FixedFloat transfer review transition: depositId={}, orderId={}, reviewUpdated={}",
                    deposit.getId(), deposit.getExchangeOrderId(), updated == 1);
        }
    }

    static void validateOrder(FixedFloatOrder order, DepositTransaction deposit) {
        if (!deposit.getExchangeOrderId().equals(order.id()) || !"float".equals(order.type())
                || order.from() == null || order.to() == null
                || !"USDTTRC".equals(order.from().code()) || !"TRX".equals(order.from().network())
                || !"TRX".equals(order.to().code()) || !"TRX".equals(order.to().network())
                || !deposit.getExchangePayoutAddress().equals(order.to().address())) {
            logger.warn("FixedFloat order mismatch: depositId={}, expectedOrderId={}, returnedOrderId={}, type={}, "
                            + "fromCode={}, fromNetwork={}, toCode={}, toNetwork={}, expectedPayoutAddress={}, actualPayoutAddress={}",
                    deposit.getId(), deposit.getExchangeOrderId(), order.id(), order.type(),
                    order.from() == null ? null : order.from().code(), order.from() == null ? null : order.from().network(),
                    order.to() == null ? null : order.to().code(), order.to() == null ? null : order.to().network(),
                    deposit.getExchangePayoutAddress(), order.to() == null ? null : order.to().address());
            throw new IllegalStateException("FixedFloat order does not match the deposit");
        }
    }
}
