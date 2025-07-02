package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.BybitConfig;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.*;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.DepositStatus;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.ManagedWalletRepo;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

// For testing purposes only
@Configuration
@Slf4j
@AllArgsConstructor
public class UsdtDepositHelperApplicationRunner implements ApplicationRunner {
    private TrongridRestClient trongridRestClient;
    private BybitConfig bybitConfig;
    private CollectionWalletRepo collectionWalletRepo;
    private ManagedWalletService managedWalletService;
    private ManagedWalletRepo managedWalletRepo;
    private BybitRestClient bybitRestClient;

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        bybitRestClient.getUsdtDeposits("0x7e4a057f5f5eaf04a3524ddecc09034fd5a041741208fe21edd8913e4f528375");
//        sendUsdtToAddress("TLwfsW16jor7K9Zt1hbRdHdrhXJUNz9mE7", 500_000_000L);

//        QuoteApply quoteApply = bybitRestClient.quoteApply(new BigDecimal("166.418"));
//        QuoteConfirm quoteConfirm = bybitRestClient.confirmQuote(quoteApply.getResult().getQuoteTxId());
//
//        while (true) {
//            QuoteCheck checkResult = bybitRestClient.convertResultQuery(quoteConfirm.getResult().getQuoteTxId());
//            String status = checkResult.getResult().getResult().getExchangeStatus(); // TODO: handle NPE
//
//            if ("processing".equals(status) || "init".equals(status)) {
//                Thread.sleep(2000);
//            } else if ("success".equals(status)) {
//                logger.info("Bybit. Successfully exchanged dep id: {}", checkResult);
//            } else {
//                logger.error("Bybit. Failed to exchanged dep id: {}",  checkResult);
//            }
//        }


//        String depositAddress = "TQ9Dg7guyk6Ndh8vEzoWCoMSUh1Vx473Lc";
//        AccountInfo accountInfo = trongridRestClient.getAccountInfo(depositAddress);

/*
        if (accountInfo == null) {
            CollectionWallet firstByIsActive = collectionWalletRepo.findFirstByIsActive(true);
            ManagedWallet managedWallet = managedWalletRepo.findById(firstByIsActive.getWalletAddress()).get();
            TreeMap<String, Object> transaction = trongridRestClient.createAccount(managedWallet.getBase58Address(), depositAddress);
            String txId = (String) transaction.get("txID");
            String signature = managedWalletService.sign(managedWallet, txId);
            transaction.put("signature", List.of(signature));
            TreeMap<String, Object> broadcastTransaction = trongridRestClient.broadcastTransaction(transaction);
            System.out.println(broadcastTransaction);
        }
*/

//        bybitRestClient.convertResultQuery("1010221327540805227360903168");

/*
        BigDecimal amount = new BigDecimal(100);
        bybitRestClient.internalTransfer("FUND", "UNIFIED", amount, "USDT");
        Thread.sleep(1000);
        PlaceOrderResponse response = bybitRestClient.placeMarketOrderTRXUSDT(amount);

        if (response.getRetCode() != 0) {
            logger.error("Bybit. Error placing market order: {}", response);
        } else {
            logger.info("Bybit. Successfully placed market order: {}", response);
            Thread.sleep(1000);
            GetOrderData orderData = bybitRestClient.getOrderStatus(response.getResult().getOrderId());
            logger.info("Bybit. Order data: {}", orderData);
        }*/
    }

    private void sendUsdtToAddress(String depositWallet, Long amount) {
        TreeMap<String, Object> responseProps = trongridRestClient.transferUsdtSmartContract(depositWallet, bybitConfig.getUsdtDepositAddress(), amount);
        LinkedHashMap<String, Object> transaction = (LinkedHashMap<String, Object>) responseProps.get("transaction");
        String txId = (String) transaction.get("txID");
        ManagedWallet managedWallet = managedWalletRepo.findById(depositWallet).get();
        String signature = managedWalletService.sign(managedWallet, txId);
        transaction.put("signature", List.of(signature));
        TreeMap<String, Object> broadcastResult = trongridRestClient.broadcastTransaction(transaction);
        String txid = (String) broadcastResult.get("txid");
    }
}
