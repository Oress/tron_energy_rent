package org.ipan.nrgyrent.telegram.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.AccountInfo;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressGet200Response;
import org.springframework.scheduling.annotation.Async;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class WithdrawalHandlerHelper {
    private final TelegramState telegramState;
    private final WithdrawViews withdrawViews;
    private final AccountApi accountApi;
    private final CollectionWalletRepo collectionWalletRepo;
    private final UserRepo userRepo;
    private final ManagedWalletRepo managedWalletRepo;
    private final ManagedWalletService managedWalletService;
    private final WithdrawalOrderService withdrawalOrderService;
    private final TronTransactionHelper tronTransactionHelper;

    @Async
    public CompletableFuture<Void> transferTrxFromCollectionWallets(Long userId, String toWallet, Long amountSun, Long fee, Boolean useGroupBalance) {
        // Manager can withdraw from the personal balance or from the group balance
        // Regular users or members of a group can withdraw only from the personal balance

        Balance withdrawBalance;

        AppUser user = userRepo.findById(userId).get();
        Balance personalBalance = user.getBalance();

        Long totalSubstractSumAmount = amountSun + fee;

        withdrawBalance = useGroupBalance ? user.getGroupBalance() : personalBalance;

        if (withdrawBalance == null) {
            logger.error("User {} has no balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: send message with update menuId, seems like very unlikely scenario
            withdrawViews.sendWithdrawalFail(userState);
            return CompletableFuture.completedFuture(null);
        }

        if (withdrawBalance.getSunBalance() < totalSubstractSumAmount) {
            logger.error("User {} has not enough balance for withdrawal", userId);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: send message with update menuId.
            withdrawViews.sendWithdrawalFailNotEnoughBalance(userState);
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
            Message newMessage = withdrawViews.sendWithdrawalFailServiceNotEnoughBalance(userState);

            telegramState.updateUserState(userState.getTelegramId(),
            userState.withMenuMessageId(newMessage.getMessageId()).withMessagesToDelete(List.of(userState.getMenuMessageId())));

            return CompletableFuture.completedFuture(null);
        }

        ManagedWallet managedWallet = managedWalletRepo.findById(walletToWithdrawFrom).get();

        WithdrawalOrder withdrawalOrder = null;
        try {
            withdrawalOrder = withdrawalOrderService.createPendingOrder(userId, useGroupBalance, amountSun, fee, toWallet);
            logger.info("Creating withdrawal request id {} user id {} amount {} balance id {} receive wallet {}", withdrawalOrder.getId(), userId, amountSun, withdrawBalance.getId(), toWallet);

            String resultingTxId = tronTransactionHelper.performTransferTransaction(
                    walletToWithdrawFrom,
                    toWallet,
                    amountSun,
                    (txId) -> managedWalletService.sign(managedWallet, txId));

            logger.info("Completing withdrawal request id {} user id {} amount {} balance id {} receive wallet {}", withdrawalOrder.getId(), userId, amountSun, withdrawBalance.getId(), toWallet);
            withdrawalOrderService.completeOrder(withdrawalOrder.getId(), resultingTxId);

            UserState userState = telegramState.getOrCreateUserState(userId);
            Message message = withdrawViews.sendWithdrawalSuccessful(userState);
            telegramState.updateUserState(userState.getTelegramId(), 
                userState.withMenuMessageId(message.getMessageId()).withMessagesToDelete(List.of(userState.getMenuMessageId())));
            logger.info("User {} has successfully withdrawn {} from {} to {}", userId, amountSun, walletToWithdrawFrom, toWallet);
        } catch (Exception e) {
            logger.error("Error while transferring TRX from collection wallets", e);
            UserState userState = telegramState.getOrCreateUserState(userId);
            // TODO: update main menu
            withdrawViews.sendWithdrawalFail(userState);
            if (withdrawalOrder != null) {
                withdrawalOrderService.refundOrder(withdrawalOrder.getId());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private String selectBalanceToWithdrawFrom(List<String> wallets, Long amountToWithdraw) {
        for (String wallet : wallets) {
            V1AccountsAddressGet200Response accountInfo = accountApi.v1AccountsAddressGet(wallet).block();
            AccountInfo data = accountInfo.getData().isEmpty() ? null : accountInfo.getData().get(0);
            Long personalSunBalance = data != null ? data.getBalance() : 0;

            if (personalSunBalance > amountToWithdraw) {
                return wallet;
            }
        }

        return null;
    }
}
