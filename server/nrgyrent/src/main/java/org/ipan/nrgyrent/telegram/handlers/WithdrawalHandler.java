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
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
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
    private final TrongridRestClient trongridRestClient;

    private final WithdrawViews withdrawViews;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.WITHDRAW_TRX)
    public void promptBalanceType(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());

        Balance groupBalance = user.getGroupBalance();
        if (groupBalance == null) {
            handleBalanceTypePersonal_promptAmount(userState, update);
        } else {
            if (!groupBalance.getIsActive()) {
                logger.error("User tries to withdraw from inactive group userstate {} group id: {}, label: {}", userState, groupBalance.getId(), groupBalance.getLabel());
                return;
            }

            AppUser manager = groupBalance.getManager();
            if (manager == null) {
                logger.error("Group has no manager group: {} userstate {}", groupBalance.getIdAndLabel(), userState);
                return;
            }

            if (!user.getTelegramId().equals(manager.getTelegramId())) {
                logger.error("Member of a group tries to withdraw funds from group. group {} userstate {}", groupBalance.getIdAndLabel(), userState);
                withdrawViews.updNotEnoughRights(userState);
                return;
            }

            handleBalanceTypeGroup_promptAmount(userState, update);
        } 
    }

    public void handleBalanceTypePersonal_promptAmount(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        Balance balance = user.getBalance();

        withdrawViews.promptAmount(userState, balance);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.USER_PROMPT_WITHDRAW_AMOUNT));
    }

    public void handleBalanceTypeGroup_promptAmount(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        Balance balance = user.getGroupBalance();

        withdrawViews.promptAmount(userState, balance);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.USER_PROMPT_WITHDRAW_AMOUNT));
    }

    @MatchState(state = States.USER_PROMPT_WITHDRAW_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmount_promptWallet(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String text = message.getText();
            
            try {
                if (text != null) {
                    text = text.replace(",", ".");
                }
                BigDecimal trxAmount = new BigDecimal(text);
                BigDecimal sunAmount = trxAmount.multiply(AppConstants.trxToSunRate);
                WithdrawParams params = telegramState.getOrCreateWithdrawParams(userState.getTelegramId());

                AppUser user = userService.getById(userState.getTelegramId());
                Balance balance = user.getBalanceToUse();

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
                    withdrawViews.promptAmountAgainNotEnoughBalance(userState, balance);
                    return;
                }
                telegramState.updateWithdrawParams(userState.getTelegramId(), params.withAmount(sunAmountLong));
            } catch (NumberFormatException e) {
                AppUser user = userService.getById(userState.getTelegramId());
                Balance balance = user.getBalanceToUse();
                withdrawViews.promptAmount(userState, balance);
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

            AccountInfo accountInfo = trongridRestClient.getAccountInfo(walletAddress);
            if (accountInfo == null) {
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
