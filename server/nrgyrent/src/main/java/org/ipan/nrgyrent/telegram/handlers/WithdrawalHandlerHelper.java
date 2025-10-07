package org.ipan.nrgyrent.telegram.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.ipan.nrgyrent.domain.exception.NotManagerException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.ManagedWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.domain.service.WithdrawalOrderService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.WithdrawViews;
import org.ipan.nrgyrent.tron.TronTransactionHelper;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class WithdrawalHandlerHelper {
    private final TelegramState telegramState;
    private final WithdrawViews withdrawViews;
    private final TrongridRestClient trongridRestClient;
    private final CollectionWalletRepo collectionWalletRepo;
    private final UserRepo userRepo;
    private final ManagedWalletRepo managedWalletRepo;
    private final ManagedWalletService managedWalletService;
    private final WithdrawalOrderService withdrawalOrderService;
    private final TronTransactionHelper tronTransactionHelper;

    // @Async
    public void transferTrxFromCollectionWallets(Long userId, String toWallet, Long amountSun, Long fee) {
        Balance withdrawBalance;

        AppUser user = userRepo.findById(userId).get();

        AccountInfo accountInfo = trongridRestClient.getAccountInfo(toWallet);
        boolean needsActivation = accountInfo == null;
        Long activationFee = needsActivation ? AppConstants.WALLET_ACTIVATION_FEE : 0;

        Long totalSubstractSumAmount = amountSun + fee + activationFee;

        withdrawBalance = user.getBalanceToUse();

        if (withdrawBalance == null) {
            logger.error("User {} has no balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: send message with update menuId, seems like very unlikely scenario
            withdrawViews.updWithdrawalFail(userState);
            return;
        }

        if (!withdrawBalance.canWithdraw(amountSun)) {
            logger.error("User's overspent their daily limit {}, can withdraw at the moment {}", userId, withdrawBalance.getDailyWithdrawalRemainingSun());
            UserState userState = telegramState.getOrCreateUserState(userId);
            withdrawViews.updWithdrawalFailNotEnoughLimit(userState, withdrawBalance.getDailyWithdrawalRemainingSun());
            return;
        }

        if (withdrawBalance.getSunBalance() < totalSubstractSumAmount) {
            logger.error("User {} has not enough balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            withdrawViews.updWithdrawalFailNotEnoughBalance(userState);
            return;
        }

        String depositAddress = withdrawBalance.getDepositAddress();
        List<CollectionWallet> collectionWallets = collectionWalletRepo.findAllByIsActive(true);
        List<String> allWallets = new ArrayList<>();
        allWallets.add(depositAddress);
        allWallets.addAll(collectionWallets.stream().map(CollectionWallet::getWalletAddress).toList());

        String walletToWithdrawFrom = selectBalanceToWithdrawFrom(allWallets, amountSun);

        if (walletToWithdrawFrom == null) {
            logger.error("Service has not enough balance for withdrawal for {}, amount {}", userId, amountSun);
            UserState userState = telegramState.getOrCreateUserState(userId);

            withdrawViews.updWithdrawalFailServiceNotEnoughBalance(userState);

            return;
        }

        ManagedWallet managedWallet = managedWalletRepo.findById(walletToWithdrawFrom).get();

        WithdrawalOrder withdrawalOrder = null;
        try {
            withdrawalOrder = withdrawalOrderService.createPendingOrder(userId, amountSun, fee, toWallet, activationFee);

            if (needsActivation) {
                tryActivateWallet(toWallet);
            }

            String resultingTxId = tronTransactionHelper.performTransferTransaction(
                    walletToWithdrawFrom,
                    toWallet,
                    amountSun,
                    (txId) -> managedWalletService.sign(managedWallet, txId));

            withdrawalOrderService.completeOrder(withdrawalOrder.getId(), resultingTxId);

            UserState userState = telegramState.getOrCreateUserState(userId);
             withdrawViews.updWithdrawalSuccessful(userState, needsActivation);
            logger.info("User {} has successfully withdrawn {} from {} to {}", userId, amountSun, walletToWithdrawFrom, toWallet);
        } catch (NotManagerException e) {
            logger.error("Member of a group tries to withdraw.", e);
            UserState userState = telegramState.getOrCreateUserState(userId);
            withdrawViews.updNotEnoughRights(userState);
        } catch (Exception e) {
            logger.error("Error while transferring TRX from collection wallets", e);
            UserState userState = telegramState.getOrCreateUserState(userId);
            withdrawViews.updWithdrawalFail(userState);
            if (withdrawalOrder != null) {
                withdrawalOrderService.refundOrder(withdrawalOrder.getId());
            }
        }

        return;
    }

    private String selectBalanceToWithdrawFrom(List<String> wallets, Long amountToWithdraw) {
        for (String wallet : wallets) {
            AccountInfo data = trongridRestClient.getAccountInfo(wallet);
            Long personalSunBalance = data != null ? data.getBalance() : 0;

            if (personalSunBalance > amountToWithdraw) {
                return wallet;
            }
        }

        return null;
    }

    public void tryActivateWallet(String address) {
        CollectionWallet firstByIsActive = collectionWalletRepo.findFirstByIsActive(true);
        ManagedWallet managedWallet = managedWalletRepo.findById(firstByIsActive.getWalletAddress()).get();
        TreeMap<String, Object> transaction = trongridRestClient.createAccount(managedWallet.getBase58Address(), address);
        String txId = (String) transaction.get("txID");
        String signature = managedWalletService.sign(managedWallet, txId);
        transaction.put("signature", List.of(signature));
        TreeMap<String, Object> broadcastTransaction = trongridRestClient.broadcastTransaction(transaction);
    }

}
