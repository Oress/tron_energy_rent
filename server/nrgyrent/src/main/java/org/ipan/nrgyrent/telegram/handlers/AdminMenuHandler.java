package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.WithdrawParams;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class AdminMenuHandler {
    private final TelegramState telegramState;
    private final RestClient restClient;
    private final TrongridRestClient trongridRestClient;
    private final CollectionWalletRepo collectionWalletRepo;
    private final UserWalletService userWalletService;
    private final AdminMenuHandlerHelper adminMenuHandlerHelper;

    private final AdminViews adminViews;

    @MatchStates({
        @MatchState(forAdmin = true, state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.ADMIN_MENU),
        @MatchState(forAdmin = true, state = States.ADMIN_VIEW_ITRX_BALANCE, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_VIEW_SWEEP_BALANCE, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_USERS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_VIEW_PROMPT_WITHDRAW_AMOUNT, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFFS, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAMS, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void handleAdminMenu(UserState userState, Update update) {
        adminViews.updMenuToAdminMenu(userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MENU));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
    public void showItrxBalance(UserState userState, Update update) {
        ApiUsageResponse apiStats = restClient.getApiStats();
        adminViews.itrxBalance(userState, apiStats);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_ITRX_BALANCE));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_SWEEP_BALANCE)
    public void showSweepBalance(UserState userState, Update update) {
        Map<CollectionWallet, Long> sweepWalletsToBalance = getSweepWalletsToBalance();
        adminViews.sweepWalletsBalance(userState, sweepWalletsToBalance);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_SWEEP_BALANCE));
    }

    private Map<CollectionWallet, Long> getSweepWalletsToBalance() {
         List<CollectionWallet> activeSweepWallets = collectionWalletRepo.findAllByIsActive(true);
        Map<CollectionWallet, Long> sweepWalletsToBalance = new HashMap<>();
        for (CollectionWallet sweepWallet : activeSweepWallets) {
            AccountInfo accountData = trongridRestClient.getAccountInfo(sweepWallet.getWalletAddress());
            logger.info("Reading sweep wallet data: {}", accountData);
            Long sunBalance = accountData != null ? accountData.getBalance() : 0;
            sweepWalletsToBalance.put(sweepWallet, sunBalance);
        }
        return sweepWalletsToBalance;
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_WITHDRAW_TRX)
    public void startAdminWithdraw_promptAmount(UserState userState, Update update) {
        WithdrawParams withdrawParams = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
        telegramState.updateWithdrawParams(userState.getTelegramId(), withdrawParams.withAmount(0L));

        adminViews.withdrawTrxPromptAmount(userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_VIEW_PROMPT_WITHDRAW_AMOUNT));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_VIEW_PROMPT_WITHDRAW_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmount_promptWallet(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String text = message.getText();
            
            try {
                BigDecimal trxAmount = new BigDecimal(text);
                BigDecimal sunAmount = trxAmount.multiply(AppConstants.trxToSunRate);
                WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());

                long sunAmountLong = sunAmount.longValue();
                if (sunAmountLong < AppConstants.MIN_WITHDRAWAL_AMOUNT) {
                    // Do nothing, keep the same state
                    return;
                }

                Map<CollectionWallet, Long> sweepWalletsToBalance = getSweepWalletsToBalance();
                Long availAmount = sweepWalletsToBalance.values().stream().mapToLong(v -> v).sum();

                if (availAmount < sunAmountLong) {
                    logger.warn("Sweep wallets do not have enough balance to withdraw. UserId: {} Avail {}, Required {}", userState.getTelegramId(), availAmount, sunAmountLong);
                    adminViews.promptAmountAgainNotEnoughBalance(userState);
                    return;
                }
                telegramState.updateWithdrawParams(userState.getTelegramId(), params.withAmount(sunAmountLong));
            } catch (NumberFormatException e) {
                adminViews.withdrawTrxPromptAmount(userState);
                return;
            }
        }

        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        adminViews.withdrawTrx(wallets, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleWalletForWithdrawalCallback(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        tryMakeTransaction(userState, callbackQuery.getData());
    }

    @MatchState(forAdmin = true, state = States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.MESSAGE)
    public void handleWalletForWithdrawalMessage(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            tryMakeTransaction(userState, message.getText());
        }
    }

    private void tryMakeTransaction(UserState userState, String walletAddress) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
            adminMenuHandlerHelper.transferTrxFromCollectionWallets(userState.getTelegramId(), walletAddress, params.getAmount());
            adminViews.withdrawTrxInProgress(userState);
        }
    }
}