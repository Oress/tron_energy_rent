package org.ipan.nrgyrent.telegram.handlers;

import java.util.UUID;

import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.telegram.AppUpdateHandler;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TransactionsHandler implements AppUpdateHandler {
    private static final int WAIT_FOR_CALLBACK = 10;
    private static final int ITRX_OK_CODE = 0;

    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final ItrxService itrxService;
    private final OrderService orderService;

    @Override
    public void handleUpdate(UserState userState, Update update) {
        switch (userState.getState()) {
            case TRANSACTION_65k:
                handleTransaction65kState(userState, update);
                break;
            case TRANSACTION_131k:
                handleTransaction131kState(userState, update);
                break;     
        }    
    }

    private void handleTransaction65kState(UserState userState, Update update) {
        handleTransactionState(userState, update, AppConstants.ENERGY_65K, AppConstants.PRICE_65K);
    }

    private void handleTransaction131kState(UserState userState, Update update) {
        handleTransactionState(userState, update, AppConstants.ENERGY_131K, AppConstants.PRICE_131K);
    }

    private void handleTransactionState(UserState userState, Update update, Integer energyAmount, Long sunAmount) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, callbackQuery.getData(), sunAmount);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, message.getText(), sunAmount);
        }
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmount, String duration, String walletAddress,
            Long sunAmount) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            telegramMessages.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            // TODO: handle exceptions, network errors, etc.
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(energyAmount, duration,
                    walletAddress);
            PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(energyAmount, duration, walletAddress,
                    correlationId);

            // Waiting WAIT_FOR_CALLBACK seconds for callback from itrx
            // if callback is not received, enqueue the request and notify the user
            // otherwise, update the menu to transaction success
            if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                return;
                // TODO: do something here
            }

            orderService.createPendingOrder(
                    AddOrUpdateOrderCommand.builder()
                            .userId(userState.getTelegramId())
                            .receiveAddress(walletAddress)
                            .energyAmount(energyAmount)
                            .duration(duration)
                            .sunAmount(sunAmount)
                            .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                            .correlationId(correlationId.toString())
                            .serial(placeOrderResponse.getSerial())
                            .build());
            // TODO: this will block the bot for incomming messages, handle it without
            // blocking.
            OrderCallbackRequest orderCallbackRequest = itrxService.getCorrelatedCallbackRequest(correlationId,
                    WAIT_FOR_CALLBACK);

            if (orderCallbackRequest != null) {
                telegramMessages.updMenuToTransactionSuccess(userState);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.TRANSACTION_SUCCESS));
                // TODO: add SUCCESSFUL DB record for transaction
            } else {
                telegramMessages.updMenuToTransactionPending(userState);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.TRANSACTION_PENDING));
            }
        }
    }
}
