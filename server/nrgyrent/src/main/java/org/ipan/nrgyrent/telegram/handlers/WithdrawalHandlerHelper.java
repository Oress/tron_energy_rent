package org.ipan.nrgyrent.telegram.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.ipan.nrgyrent.domain.exception.NotManagerException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.WithdrawalOrder;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.ManagedWalletRepo;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.domain.service.WithdrawalOrderService;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.WithdrawViews;
import org.ipan.nrgyrent.tron.TronTransactionHelper;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.springframework.scheduling.annotation.Async;

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

    @Async
    public CompletableFuture<Void> transferTrxFromCollectionWallets(Long userId, String toWallet, Long amountSun, Long fee, Boolean useGroupBalance) {
        Balance withdrawBalance;

        AppUser user = userRepo.findById(userId).get();
        Balance personalBalance = user.getBalance();

        Long totalSubstractSumAmount = amountSun + fee;

        withdrawBalance = useGroupBalance ? user.getGroupBalance() : personalBalance;

        if (withdrawBalance == null) {
            logger.error("User {} has no balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: send message with update menuId, seems like very unlikely scenario
            withdrawViews.updWithdrawalFail(userState);
            return CompletableFuture.completedFuture(null);
        }

        if (withdrawBalance.getSunBalance() < totalSubstractSumAmount) {
            logger.error("User {} has not enough balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: send message with update menuId.
            withdrawViews.updWithdrawalFailNotEnoughBalance(userState);
            return CompletableFuture.completedFuture(null);
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

            return CompletableFuture.completedFuture(null);
        }

        ManagedWallet managedWallet = managedWalletRepo.findById(walletToWithdrawFrom).get();

        WithdrawalOrder withdrawalOrder = null;
        try {
            withdrawalOrder = withdrawalOrderService.createPendingOrder(userId, amountSun, fee, toWallet);

            String resultingTxId = tronTransactionHelper.performTransferTransaction(
                    walletToWithdrawFrom,
                    toWallet,
                    amountSun,
                    (txId) -> managedWalletService.sign(managedWallet, txId));

            withdrawalOrderService.completeOrder(withdrawalOrder.getId(), resultingTxId);

            UserState userState = telegramState.getOrCreateUserState(userId);
             withdrawViews.updWithdrawalSuccessful(userState);
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
        return CompletableFuture.completedFuture(null);
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
}
