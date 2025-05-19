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
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.AccountInfo;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressGet200Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AdminMenuHandler implements AppUpdateHandler {
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

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case ADMIN_MENU:
                processMainMenuCallback(userState, update);
                break;

            case ADMIN_VIEW_PROMPT_WITHDRAW_WALLET:
                handleWalletForWithdrawal(userState, update);
                break;
        }
    }

    private void handleWalletForWithdrawal(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, callbackQuery.getData());
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, message.getText());
        }
    }

    private void tryMakeTransaction(UserState userState, String walletAddress) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            adminMenuHandlerHelper.transferTrxFromCollectionWallets(userState.getTelegramId(), walletAddress);
            adminViews.withdrawTrxInProgress(userState);
        }
    }

    private void processMainMenuCallback(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();
            logger.info("Received callback query: {}", data);

            if (InlineMenuCallbacks.MANAGE_GROUPS.equals(data)) {
                manageGroupNewGroupView.updMenuToManageGroupsMenu(callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_GROUPS));
            } else if (InlineMenuCallbacks.MANAGE_USERS.equals(data)) {
                Page<AppUser> firstPage = appUserRepo.findAllByTelegramUsernameContainingIgnoreCaseOrderByTelegramId("", PageRequest.of(0, 10));
                manageUserActionsView.updMenuToManageUsersSearchResult(firstPage, userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_USERS));
            } else if (InlineMenuCallbacks.MANAGE_ITRX_BALANCE.equals(data)) {
                ApiUsageResponse apiStats = restClient.getApiStats();
                adminViews.itrxBalance(callbackQuery, apiStats);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_VIEW_ITRX_BALANCE));
            } else if (InlineMenuCallbacks.MANAGE_SWEEP_BALANCE.equals(data)) {
                List<CollectionWallet> activeSweepWallets = collectionWalletRepo.findAllByIsActive(true);
                Map<CollectionWallet, Long> sweepWalletsToBalance = new HashMap<>();
                for (CollectionWallet sweepWallet : activeSweepWallets) {
                    V1AccountsAddressGet200Response accountInfo = accountApi.v1AccountsAddressGet(sweepWallet.getWalletAddress()).block();
                    AccountInfo accountData = accountInfo.getData().isEmpty() ? null : accountInfo.getData().get(0);
                    Long sunBalance = accountData != null ? accountData.getBalance() : 0;
                    sweepWalletsToBalance.put(sweepWallet, sunBalance);
                }
                adminViews.sweepWalletsBalance(callbackQuery, sweepWalletsToBalance);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_VIEW_SWEEP_BALANCE));
            } else if (InlineMenuCallbacks.MANAGE_WITHDRAW_TRX.equals(data)) {
                TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
                telegramState.updateTransactionParams(userState.getTelegramId(),
                        transactionParams.withGroupBalance(true));

                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                adminViews.withdrawTrx(wallets, callbackQuery);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_VIEW_PROMPT_WITHDRAW_WALLET));
            }
        }
    }
}