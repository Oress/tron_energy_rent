package org.ipan.nrgyrent.telegram.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.AccountInfo;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressGet200Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final AccountApi accountApi;
    private final CollectionWalletRepo collectionWalletRepo;
    private final AppUserRepo appUserRepo;
    private final UserWalletService userWalletService;
    private final AdminMenuHandlerHelper adminMenuHandlerHelper;

    private final ManageGroupNewGroupView manageGroupNewGroupView;
    private final ManageUserActionsView manageUserActionsView;
    private final AdminViews adminViews;

    @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_GROUPS)
    public void showManageGroupsMenu(UserState userState, Update update) {
        manageGroupNewGroupView.updMenuToManageGroupsMenu(update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS));
    }

    @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_USERS)
    public void showManageUsersMenu(UserState userState, Update update) {
        Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId("",
                PageRequest.of(0, 10));
        manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USERS));
    }

    @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
    public void showItrxBalance(UserState userState, Update update) {
        ApiUsageResponse apiStats = restClient.getApiStats();
        adminViews.itrxBalance(update.getCallbackQuery(), apiStats);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_ITRX_BALANCE));
    }

    @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
    public void showSweepBalance(UserState userState, Update update) {
        List<CollectionWallet> activeSweepWallets = collectionWalletRepo.findAllByIsActive(true);
        Map<CollectionWallet, Long> sweepWalletsToBalance = new HashMap<>();
        for (CollectionWallet sweepWallet : activeSweepWallets) {
            V1AccountsAddressGet200Response accountInfo = accountApi
                    .v1AccountsAddressGet(sweepWallet.getWalletAddress()).block();
            AccountInfo accountData = accountInfo.getData().isEmpty() ? null : accountInfo.getData().get(0);
            Long sunBalance = accountData != null ? accountData.getBalance() : 0;
            sweepWalletsToBalance.put(sweepWallet, sunBalance);
        }
        adminViews.sweepWalletsBalance(update.getCallbackQuery(), sweepWalletsToBalance);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_SWEEP_BALANCE));
    }

    @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
    public void showWithdrawTrxMenu(UserState userState, Update update) {
        TransactionParams transactionParams = telegramState
                .getOrCreateTransactionParams(userState.getTelegramId());
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams.withGroupBalance(true));

        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        adminViews.withdrawTrx(wallets, update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET));
    }

    @MatchState(state = States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleWalletForWithdrawalCallback(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        tryMakeTransaction(userState, callbackQuery.getData());
    }

    @MatchState(state = States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET, updateTypes = UpdateType.MESSAGE)
    public void handleWalletForWithdrawalMessage(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            tryMakeTransaction(userState, message.getText());
        }
    }

    private void tryMakeTransaction(UserState userState, String walletAddress) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            adminMenuHandlerHelper.transferTrxFromCollectionWallets(userState.getTelegramId(), walletAddress);
            adminViews.withdrawTrxInProgress(userState);
        }
    }
}