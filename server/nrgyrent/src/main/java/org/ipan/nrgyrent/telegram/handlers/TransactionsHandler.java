package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.UUID;

import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.TransactionsViews;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TransactionsHandler implements AppUpdateHandler {
    private static final int ITRX_OK_CODE = 0;

    private final TelegramState telegramState;
    private final TransactionsViews transactionsViews;
    private final ItrxService itrxService;
    private final OrderService orderService;
    private final UserWalletService userWalletService;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case TRANSACTION_PROMPT_BALANCE_TYPE:
                handlePromptBalanceType(userState, update);
                break;
            case TRANSACTION_PROMPT_WALLET:
                TransactionParams transactionParams = telegramState
                        .getOrCreateTransactionParams(userState.getTelegramId());

                Integer energyAmount = transactionParams.getEnergyAmount();
                Long price = switch (energyAmount) {
                    case AppConstants.ENERGY_131K -> AppConstants.PRICE_131K;
                    case AppConstants.ENERGY_65K -> AppConstants.PRICE_65K;
                    default -> {
                        throw new IllegalStateException("Unexpected value: " + energyAmount);
                    }
                };
                handlePromptWallet(userState, update, energyAmount, price, transactionParams.getGroupBalance());
                break;
        }
    }

    public void promptWalletDependingOnEnergyAmount(CallbackQuery callbackQuery, List<UserWallet> wallets, Integer energyAmount) {
        if (energyAmount == AppConstants.ENERGY_131K) {
            transactionsViews.updMenuToTransaction131kMenu(wallets, callbackQuery);
        } else {
            transactionsViews.updMenuToTransaction65kMenu(wallets, callbackQuery);
        }

    }

    private void handlePromptWallet(UserState userState, Update update, Integer energyAmount, Long sunAmount, boolean useGroupWallet) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, callbackQuery.getData(), sunAmount, useGroupWallet);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, message.getText(), sunAmount, useGroupWallet);
        }
    }

    private void handlePromptBalanceType(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            String data = callbackQuery.getData();

            if (InlineMenuCallbacks.TRANSACTION_BALANCE_GROUP.equals(data)) {
                TransactionParams transactionParams = telegramState
                        .getOrCreateTransactionParams(userState.getTelegramId());
                telegramState.updateTransactionParams(userState.getTelegramId(),
                        transactionParams.withGroupBalance(true));
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.TRANSACTION_PROMPT_WALLET));

                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                promptWalletDependingOnEnergyAmount(callbackQuery, wallets, transactionParams.getEnergyAmount());
            } else if (InlineMenuCallbacks.TRANSACTION_BALANCE_PERSONAL.equals(data)) {
                TransactionParams transactionParams = telegramState
                        .getOrCreateTransactionParams(userState.getTelegramId());
                telegramState.updateTransactionParams(userState.getTelegramId(),
                        transactionParams.withGroupBalance(false));
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.TRANSACTION_PROMPT_WALLET));

                List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
                promptWalletDependingOnEnergyAmount(callbackQuery, wallets, transactionParams.getEnergyAmount());
            }
        }
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmount, String duration, String walletAddress,
            Long sunAmount, boolean useGroupWallet) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            transactionsViews.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(energyAmount, duration,
                    walletAddress);
            try {
                orderService.createPendingOrder(
                        AddOrUpdateOrderCommand.builder()
                                .userId(userState.getTelegramId())
                                .useGroupWallet(useGroupWallet)
                                .receiveAddress(walletAddress)
                                .energyAmount(energyAmount)
                                .duration(duration)
                                .sunAmount(sunAmount)
                                .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                                .correlationId(correlationId.toString())
                                // .serial(placeOrderResponse.getSerial())
                                .build());

                // TODO: handle exceptions, network errors, etc.
                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(energyAmount, duration, walletAddress,
                        correlationId);

                if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                    return;
                    // TODO: do something here
                }

                transactionsViews.updMenuToTransactionPending(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PENDING));
            } catch (NotEnoughBalanceException e) {
                transactionsViews.notEnoughBalance(userState);
            }
        }
    }
}
