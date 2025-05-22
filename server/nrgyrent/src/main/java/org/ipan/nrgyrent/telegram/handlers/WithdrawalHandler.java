package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;
import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.WithdrawParams;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.WithdrawViews;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressGet200Response;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class WithdrawalHandler {
    private final TelegramState telegramState;
    private final UserWalletService userWalletService;
    private final UserService userService;
    private final WithdrawalHandlerHelper withdrawalHandlerHelper;
    private final AccountApi accountApi;

    private final WithdrawViews withdrawViews;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.WITHDRAW_TRX)
    public void promptBalanceType(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());

        if (user.getGroupBalance() == null) {
            handleBalanceTypePersonal_promptAmount(userState, update);
        } else {
            withdrawViews.promptBalanceType(update.getCallbackQuery());
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.USER_PROMPT_WITHDRAW_BALANCE_TYPE));
        }
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_BALANCE_TYPE, callbackData = InlineMenuCallbacks.WITHDRAW_BALANCE_PERSONAL)
    public void handleBalanceTypePersonal_promptAmount(UserState userState, Update update) {
        WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
        telegramState.updateWithdrawParams(userState.getTelegramId(), params.withGroupBalance(false));

        withdrawViews.promptAmount(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.USER_PROMPT_WITHDRAW_AMOUNT));
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_BALANCE_TYPE, callbackData = InlineMenuCallbacks.WITHDRAW_BALANCE_GROUP)
    public void handleBalanceTypeGroup_promptAmount(UserState userState, Update update) {
        WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
        telegramState.updateWithdrawParams(userState.getTelegramId(), params.withGroupBalance(true));

        // TODO: fetch balance and show the max withdrawal amount
        withdrawViews.promptAmount(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.USER_PROMPT_WITHDRAW_AMOUNT));
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmount_promptWallet(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String text = message.getText();
            
            try {
                BigDecimal trxAmount = new BigDecimal(text);
                BigDecimal sunAmount = trxAmount.multiply(AppConstants.trxToSunRate);
                WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());

                AppUser user = userService.getById(userState.getTelegramId());
                Balance balance = params.getGroupBalance() ? user.getGroupBalance() : user.getBalance();

                if (balance == null) {
                    logger.error("User withdrawing TRX {} has no balance params: {}", userState.getTelegramId(), params);
                    throw new IllegalStateException("User has no balance");
                }
                
                long sunAmountLong = sunAmount.longValue();
                if (sunAmountLong < AppConstants.MIN_WITHDRAWAL_AMOUNT) {
                    // Do nothing, keep the same state
                    return;
                }

                if (balance.getSunBalance() < sunAmountLong + AppConstants.WITHDRAWAL_FEE) {
                    logger.warn("User {} has not enough balance for withdrawal, balance: {}, required: {}, fee: {}", userState.getTelegramId(), balance.getSunBalance(), sunAmountLong, AppConstants.WITHDRAWAL_FEE);
                    withdrawViews.promptAmountAgainNotEnoughBalance(userState);
                    return;
                }
                telegramState.updateWithdrawParams(userState.getTelegramId(), params.withAmount(sunAmountLong));
            } catch (NumberFormatException e) {
                withdrawViews.promptAmount(update.getCallbackQuery());
                return;
            }
        }

        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        withdrawViews.promptWallets(wallets, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.USER_PROMPT_WITHDRAW_WALLET));
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleWalletForWithdrawalCallback(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
        tryMakeTransaction(userState, callbackQuery.getData(), params.getAmount(), params.getGroupBalance());
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.MESSAGE)
    public void handleWalletForWithdrawalMessage(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());
            tryMakeTransaction(userState, message.getText(), params.getAmount(), params.getGroupBalance());
        }
    }

    private void tryMakeTransaction(UserState userState, String walletAddress, Long sunAmount, Boolean groupBalance) {
        if (WalletTools.isValidTronAddress(walletAddress)) {

            V1AccountsAddressGet200Response accountInfo = accountApi.v1AccountsAddressGet(walletAddress).block();
            if (accountInfo == null || accountInfo.getData().isEmpty()) {
                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                withdrawViews.withdrawTrxInactiveWallet(wallets, userState);
                return;
            }

            withdrawalHandlerHelper.transferTrxFromCollectionWallets(
                    userState.getTelegramId(),
                    walletAddress,
                    sunAmount,
                    AppConstants.WITHDRAWAL_FEE,
                    groupBalance);
            withdrawViews.withdrawTrxInProgress(userState);
        }
    }
}
