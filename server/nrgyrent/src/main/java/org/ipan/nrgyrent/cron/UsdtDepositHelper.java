package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.BybitConfig;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.*;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

@Configuration
@Slf4j
@AllArgsConstructor
public class UsdtDepositHelper {
    private final BalanceRepo balanceRepo;
    private final AppUserRepo userRepo;
    private final CollectionWalletRepo collectionWalletRepo;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final ItrxService itrxService;
    private TrongridRestClient trongridRestClient;
    private EnergyService energyService;
    private BybitConfig bybitConfig;
    private BybitRestClient bybitRestClient;
    private ManagedWalletService managedWalletService;
    private ManagedWalletRepo managedWalletRepo;
    private DepositTransactionRepo depositTransactionRepo;

    @SneakyThrows
    public GetOrderData placeBuyOrderUsdtToTrx(DepositTransaction depositTransaction) {
        GetOrderData result = null;

        // leave 1 cent for cheaper energy in the future
        BigDecimal amount = BigDecimal.valueOf(depositTransaction.getOriginalAmount())
                .divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.DOWN)
                .subtract(new BigDecimal("0.01"));

        InternalTransferResponse internalTransferResponse = bybitRestClient.internalTransfer("FUND", "UNIFIED", amount, "USDT");

        if (internalTransferResponse.getRetCode() != 0) {
            logger.error("Bybit. Error placing internal transfer: {}", internalTransferResponse);
            depositTransaction.setStatus(DepositStatus.USDT_MOVED_TO_UTA_FAILED);
            depositTransactionRepo.save(depositTransaction);
            return null;
        }
        depositTransaction.setStatus(DepositStatus.USDT_MOVED_TO_UTA);
        depositTransaction.setBybitTransferId(internalTransferResponse.getResult().getTransferId());
        depositTransactionRepo.save(depositTransaction);
        logger.info("Bybit. Successfully placed internal transfer : {}", internalTransferResponse);

        // This is bad as f
        Thread.sleep(5000);

        PlaceOrderResponse response = bybitRestClient.placeMarketOrderTRXUSDT(amount);
        if (response.getRetCode() != 0) {
            logger.error("Bybit. Error placing market order: {}", response);
            depositTransaction.setStatus(DepositStatus.USDT_MARKET_ORDER_PLACED_FAILED);
            depositTransactionRepo.save(depositTransaction);
            return null;
        }
        logger.info("Bybit. Successfully placed market order: {}", response);
        depositTransaction.setBybitOrderId(response.getResult().getOrderId());
        depositTransaction.setStatus(DepositStatus.USDT_MARKET_ORDER_PLACED);
        depositTransactionRepo.save(depositTransaction);

        Thread.sleep(5000);
        result = bybitRestClient.getOrderStatus(response.getResult().getOrderId());
        if (!"Filled".equals(result.getOrderStatus())) {
            logger.error("Bybit. Order has unexpected status: {}", result);
            depositTransaction.setStatus(DepositStatus.USDT_MARKET_ORDER_UNEXPECTED_STATUS);
            depositTransactionRepo.save(depositTransaction);
            return null;
        }

        depositTransaction.setStatus(DepositStatus.USDT_ORDER_COMPLETED);
        depositTransactionRepo.save(depositTransaction);

        BigDecimal trxTotal = new BigDecimal(result.getCumExecQty());
        BigDecimal bybitFeeTrx = new BigDecimal(result.getCumExecFee());
        BigDecimal deposit = trxTotal
                .subtract(bybitFeeTrx)  // Bybit fee ~ 0.1%
                .subtract(BigDecimal.ONE.divide(new BigDecimal(result.getAvgPrice()), 6, RoundingMode.DOWN)) // 1 USDT for renting energy
                .add(new BigDecimal("0.01").divide(new BigDecimal(result.getAvgPrice()), 6, RoundingMode.DOWN)); // 0.01 USDT compensation left on the account
        long depositSun = deposit.multiply(AppConstants.trxToSunRate).longValue();

        depositTransaction.setBybitFeeSun(bybitFeeTrx.multiply(AppConstants.trxToSunRate).longValue());
        depositTransaction.setTrxToUsdtRate(new BigDecimal(result.getAvgPrice()));
        depositTransaction.setAmount(depositSun);
        depositTransactionRepo.save(depositTransaction);
        Balance byDepositAddress = balanceRepo.findByDepositAddress(depositTransaction.getWalletTo());
        byDepositAddress.makeDeposit(depositSun);
        balanceRepo.save(byDepositAddress);

        AppUser byBalanceId = userRepo.findByBalanceId(byDepositAddress.getId());
        UserState orCreateUserState = telegramState.getOrCreateUserState(byBalanceId.getTelegramId());
        telegramMessages.sendTopupUsdtNotification(orCreateUserState, depositTransaction);

        logger.info("Bybit. Order data: {}", result);
        return result;
    }

    public void tryActivateWallet(DepositTransaction depositTransaction) {
        String depositAddress = depositTransaction.getWalletTo();
        AccountInfo accountInfo = trongridRestClient.getAccountInfo(depositAddress);

        // means that an account is not activated yet
        if (accountInfo == null) {
            CollectionWallet firstByIsActive = collectionWalletRepo.findFirstByIsActive(true);
            ManagedWallet managedWallet = managedWalletRepo.findById(firstByIsActive.getWalletAddress()).get();
            TreeMap<String, Object> transaction = trongridRestClient.createAccount(managedWallet.getBase58Address(), depositAddress);
            String txId = (String) transaction.get("txID");
            String signature = managedWalletService.sign(managedWallet, txId);
            transaction.put("signature", List.of(signature));
            TreeMap<String, Object> broadcastTransaction = trongridRestClient.broadcastTransaction(transaction);

            depositTransaction.setActivationFeeSun(1_100_000L);
            depositTransactionRepo.save(depositTransaction);
        }
    }

    public void rentEnergyForUsdtTransfer(DepositTransaction depositTransaction) {
        EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(null, AppConstants.DURATION_1H, bybitConfig.getUsdtDepositAddress());
        Order order = energyService.tryMakeSystemTransaction(estimateOrderResponse.getEnergy_amount(), AppConstants.DURATION_1H, depositTransaction.getWalletTo());
        depositTransaction.setStatus(DepositStatus.USDT_ENERGY_RENTED);
        depositTransaction.setSystemOrder(order);
        depositTransactionRepo.save(depositTransaction);
    }

    public String transferUsdtToBybit(DepositTransaction depositTransaction) {
        String depositWallet = depositTransaction.getWalletTo();
        // 0.01 usdt should be left in account so that the following transfers would cost 2x less
        // the bot's commission covers it
        Long oneCent = 10_000L;
        TreeMap<String, Object> responseProps = trongridRestClient.transferUsdtSmartContract(depositWallet,
                bybitConfig.getUsdtDepositAddress(),
                depositTransaction.getOriginalAmount() - oneCent);
        LinkedHashMap<String, Object> transaction = (LinkedHashMap<String, Object>) responseProps.get("transaction");
        String txId = (String) transaction.get("txID");
        ManagedWallet managedWallet = managedWalletRepo.findById(depositWallet).get();
        String signature = managedWalletService.sign(managedWallet, txId);
        transaction.put("signature", List.of(signature));
        TreeMap<String, Object> broadcastResult = trongridRestClient.broadcastTransaction(transaction);
        String txid = (String) broadcastResult.get("txid");
        depositTransaction.setBybitUsdtTx(txid);

        if (txid != null && !txid.isEmpty()) {
            logger.info("Transaction successful: {}", txid);
            depositTransaction.setStatus(DepositStatus.USDT_TRANSFERRED_TO_BYBIT);
            depositTransactionRepo.save(depositTransaction);
        } else {
            depositTransaction.setStatus(DepositStatus.USDT_TRANSFERRED_TO_BYBIT_FAILED);
            depositTransactionRepo.save(depositTransaction);
            logger.error("Transaction failed: {}", broadcastResult);
        }

        return txid;
    }

/*    @SneakyThrows
    public QuoteCheck exchangeUsdtToTrx(DepositTransaction depositTransaction) {
        QuoteApply quoteApply = bybitRestClient.quoteApply(BigDecimal.valueOf(depositTransaction.getOriginalAmount()).divide(BigDecimal.valueOf(1_000_000), 2, RoundingMode.DOWN));

        if (quoteApply.getRetCode() != 0) {
            logger.error("Bybit. Error applying quote for dep id:{},  {}", depositTransaction.getId(), quoteApply);
            depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_FAILED_TO_START);
            depositTransactionRepo.save(depositTransaction);
        }

        logger.info("Bybit. Successfully applied quote for dep id: {},  {}", depositTransaction.getId(), quoteApply);
        depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_STARTED);
//        depositTransaction.setQuoteTxId(quoteApply.getResult().getQuoteTxId());
        depositTransactionRepo.save(depositTransaction);

        QuoteConfirm quoteConfirm = bybitRestClient.confirmQuote(quoteApply.getResult().getQuoteTxId());

        if (quoteConfirm.getRetCode() != 0) {
            logger.error("Bybit. Error confirming quote for dep id:{},  {}", depositTransaction.getId(), quoteConfirm);
            depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_FAILED_TO_CONFIRM);
            depositTransactionRepo.save(depositTransaction);
        }

        logger.info("Bybit. Successfully confirmed quote for dep id: {},  {}", depositTransaction.getId(), quoteConfirm);
        depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_IN_PROGRESS);
        depositTransactionRepo.save(depositTransaction);

        while (true) {
            QuoteCheck checkResult = bybitRestClient.convertResultQuery(quoteConfirm.getResult().getQuoteTxId());
            String status = checkResult.getResult().getResult().getExchangeStatus();

            if ("processing".equals(status) || "init".equals(status)) {
                Thread.sleep(3000);
            } else if ("success".equals(status)) {
                logger.info("Bybit. Successfully exchanged dep id: {},  {}", depositTransaction.getId(), checkResult);
                depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_COMPLETED);
                depositTransaction.setAmount(new BigDecimal(checkResult.getResult().getResult().getToAmount()).multiply(AppConstants.trxToSunRate).longValue());
                depositTransaction.setUsdtToTrxRate(new BigDecimal(checkResult.getResult().getResult().getConvertRate()));
                depositTransactionRepo.save(depositTransaction);
                Balance byDepositAddress = balanceRepo.findByDepositAddress(depositTransaction.getWalletTo());
                byDepositAddress.makeDeposit(depositTransaction.getAmount());
                balanceRepo.save(byDepositAddress);
                return checkResult;
            } else {
                logger.error("Bybit. Failed to exchanged dep id: {},  {}", depositTransaction.getId(), checkResult);
                depositTransaction.setStatus(DepositStatus.USDT_EXCHANGE_FAILED);
                depositTransactionRepo.save(depositTransaction);
                return checkResult;
            }
        }
    }*/
}
