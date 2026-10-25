package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.WithdrawResponse;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.BybitWithdrawParams;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.BybitWithdrawViews;
import org.telegram.telegrambots.meta.api.objects.Update;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class BybitWithdrawalHandler {
    private final TelegramState telegramState;
    private final CollectionWalletRepo collectionWalletRepo;
    private final BybitRestClient bybitRestClient;
    private final BybitWithdrawViews views;
    private final AdminViews adminViews;

    @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_BYBIT_WITHDRAW)
    public void promptAmount(UserState userState, Update update) {
        telegramState.removeBybitWithdrawParams(userState.getTelegramId());
        CollectionWallet wallet = collectionWalletRepo.findFirstByIsActive(true);
        if (wallet == null || wallet.getWalletAddress() == null || wallet.getWalletAddress().length() != 34
                || !WalletTools.isValidTronAddress(wallet.getWalletAddress())) {
            setState(userState, States.ADMIN_BYBIT_WITHDRAW_RESULT);
            views.noWallet(userState);
            return;
        }
        telegramState.updateBybitWithdrawParams(userState.getTelegramId(),
                new BybitWithdrawParams(wallet.getId(), wallet.getWalletAddress(), null, null));
        setState(userState, States.ADMIN_BYBIT_WITHDRAW_AMOUNT);
        views.promptAmount(userState, wallet.getWalletAddress(), false);
    }

    @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmount(UserState userState, Update update) {
        BybitWithdrawParams params = telegramState.getBybitWithdrawParams(userState.getTelegramId());
        if (params == null) {
            promptAmount(userState, update);
            return;
        }
        String text = update.getMessage().hasText() ? update.getMessage().getText().trim().replace(',', '.') : "";
        // Reject excess precision instead of silently rounding the requested transfer.
        if (text.length() > 40 || !text.matches("[0-9]+(?:\\.[0-9]{1,6})?")
                || new BigDecimal(text).signum() <= 0) {
            views.promptAmount(userState, params.address(), true);
            return;
        }
        BybitWithdrawParams confirmed = new BybitWithdrawParams(params.walletId(), params.address(),
                new BigDecimal(text).stripTrailingZeros(), UUID.randomUUID().toString().replace("-", ""));
        telegramState.updateBybitWithdrawParams(userState.getTelegramId(), confirmed);
        setState(userState, States.ADMIN_BYBIT_WITHDRAW_CONFIRM);
        views.confirm(userState, confirmed);
    }

    @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_CONFIRM, updateTypes = UpdateType.CALLBACK_QUERY)
    public void confirm(UserState userState, Update update) {
        BybitWithdrawParams params = telegramState.getBybitWithdrawParams(userState.getTelegramId());
        String callback = update.getCallbackQuery().getData();
        if (params == null || params.requestId() == null
                || !Objects.equals(callback, InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId())) {
            return;
        }
        // Atomically consume this exact request before any external action. Old buttons cannot submit a newer request.
        if (!telegramState.consumeBybitWithdrawParams(userState.getTelegramId(), params)) {
            return;
        }
        setState(userState, States.ADMIN_BYBIT_WITHDRAW_RESULT);
        CollectionWallet wallet = collectionWalletRepo.findById(params.walletId()).orElse(null);
        if (wallet == null || !Boolean.TRUE.equals(wallet.getIsActive())
                || !Objects.equals(wallet.getWalletAddress(), params.address())) {
            views.noWallet(userState);
            return;
        }

        logger.info("Admin {} requested Bybit withdrawal: {} TRX to {}, requestId={}",
                userState.getTelegramId(), params.amount(), params.address(), params.requestId());
        WithdrawResponse response;
        try {
            response = bybitRestClient.withdrawTrx(params.address(), params.amount(), params.requestId());
        } catch (Exception e) {
            logger.error("Bybit withdrawal outcome unknown, requestId={}", params.requestId(), e);
            views.unknown(userState, params.requestId());
            return;
        }
        // Keep Telegram rendering outside the API try/catch: a display failure must not change the reported API outcome.
        if (response == null || response.getRetCode() == null) {
            views.unknown(userState, params.requestId());
        } else if (response.getRetCode() != 0) {
            logger.warn("Bybit withdrawal rejected, requestId={}, code={}, message={}",
                    params.requestId(), response.getRetCode(), response.getRetMsg());
            views.rejected(userState, response.getRetCode(), response.getRetMsg());
        } else if (response.getResult() == null || response.getResult().getId() == null
                || response.getResult().getId().isBlank()) {
            views.unknown(userState, params.requestId());
        } else {
            logger.info("Bybit withdrawal accepted, requestId={}, withdrawalId={}",
                    params.requestId(), response.getResult().getId());
            views.submitted(userState, params, response.getResult().getId());
        }
    }

    @MatchStates({
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_AMOUNT, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO),
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_RESULT, callbackData = InlineMenuCallbacks.GO_BACK)
    })
    public void back(UserState userState, Update update) {
        telegramState.removeBybitWithdrawParams(userState.getTelegramId());
        setState(userState, States.ADMIN_MENU);
        adminViews.updMenuToAdminMenu(userState);
    }

    @MatchStates({
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_AMOUNT, callbackData = InlineMenuCallbacks.TO_MAIN_MENU),
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_CONFIRM, callbackData = InlineMenuCallbacks.TO_MAIN_MENU),
        @MatchState(forAdmin = true, state = States.ADMIN_BYBIT_WITHDRAW_RESULT, callbackData = InlineMenuCallbacks.TO_MAIN_MENU)
    })
    public void clear(UserState userState, Update update) {
        telegramState.removeBybitWithdrawParams(userState.getTelegramId());
    }

    private void setState(UserState userState, States state) {
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(state));
    }
}
