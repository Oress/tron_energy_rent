package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.UUID;

import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.InactiveAddressException;
import org.ipan.nrgyrent.itrx.ItrxInsufficientFundsException;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.TransactionsViews;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class TransactionsHandler {
    private static final int ITRX_OK_CODE = 0;

    private final TelegramState telegramState;
    private final TransactionsViews transactionsViews;
    private final ItrxService itrxService;
    private final UserService userService;
    private final OrderService orderService;
    private final UserWalletService userWalletService;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_65k)
    public void handleTransaction65k(UserState userState, Update update) {
        proceedToTransactions(userState, update.getCallbackQuery(), AppConstants.ENERGY_65K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_131k)
    public void handleTransaction131k(UserState userState, Update update) {
        proceedToTransactions(userState, update.getCallbackQuery(), AppConstants.ENERGY_131K);
    }

    private void proceedToTransactions(UserState userState, CallbackQuery callbackQuery, Integer energyAmount) {
        AppUser byId = userService.getById(userState.getTelegramId());
        // If no group balance, proceed to
        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
        boolean useGroupBalance = true;
        if (byId.getGroupBalance() == null) {
            useGroupBalance = false;
            List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());

            if (energyAmount == AppConstants.ENERGY_131K) {
                transactionsViews.updMenuToTransaction131kMenu(wallets, callbackQuery);
            } else {
                transactionsViews.updMenuToTransaction65kMenu(wallets, callbackQuery);
            }

            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.TRANSACTION_PROMPT_WALLET));
        } else {
            transactionsViews.updMenuToPromptBalanceType(callbackQuery);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.TRANSACTION_PROMPT_BALANCE_TYPE));
        }
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams.withGroupBalance(useGroupBalance).withEnergyAmount(energyAmount));
    }

    @MatchStates({
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.CALLBACK_QUERY),
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.MESSAGE)
    })
    public void processWalletForTransaction(UserState userState, Update update) {
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
    }

    private void handlePromptWallet(UserState userState, Update update, Integer energyAmount, Long sunAmount,
            boolean useGroupWallet) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, callbackQuery.getData(), sunAmount,
                    useGroupWallet);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, message.getText(), sunAmount,
                    useGroupWallet);
        }
    }

    @MatchState(state = States.TRANSACTION_PROMPT_BALANCE_TYPE, callbackData = InlineMenuCallbacks.TRANSACTION_BALANCE_GROUP)
    public void selectGroupBalanceTypeForTransaction(UserState userState, Update update) {
        TransactionParams transactionParams = telegramState
                .getOrCreateTransactionParams(userState.getTelegramId());
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams.withGroupBalance(true));
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.TRANSACTION_PROMPT_WALLET));

        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        promptWalletDependingOnEnergyAmount(update.getCallbackQuery(), wallets, transactionParams.getEnergyAmount());
    }

    @MatchState(state = States.TRANSACTION_PROMPT_BALANCE_TYPE, callbackData = InlineMenuCallbacks.TRANSACTION_BALANCE_PERSONAL)
    public void selectPersonalBalanceTypeForTransaction(UserState userState, Update update) {
        TransactionParams transactionParams = telegramState
                .getOrCreateTransactionParams(userState.getTelegramId());
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams.withGroupBalance(false));
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.TRANSACTION_PROMPT_WALLET));

        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        promptWalletDependingOnEnergyAmount(update.getCallbackQuery(), wallets, transactionParams.getEnergyAmount());
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmount, String duration, String walletAddress,
            Long sunAmount, boolean useGroupWallet) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            transactionsViews.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(energyAmount, duration,
                    walletAddress);

            Order pendingOrder = null;
            try {
                pendingOrder = orderService.createPendingOrder(
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

                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(energyAmount, duration, walletAddress,
                        correlationId);

                if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                    transactionsViews.somethingWentWrong(userState);
                    telegramState.updateUserState(userState.getTelegramId(),
                            userState.withState(States.TRANSACTION_ERROR));
                    return;
                }

                transactionsViews.updMenuToTransactionPending(userState);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.TRANSACTION_PENDING));
            } catch (NotEnoughBalanceException e) { // Not enough balance on our service
                transactionsViews.notEnoughBalance(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
            } catch (InactiveAddressException e) {
                transactionsViews.transactionToInactiveWallet(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));

                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
            } catch (ItrxInsufficientFundsException e) {
                transactionsViews.itrxBalanceNotEnoughFunds(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));

                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
            } catch (Exception e) {
                logger.error("Error during transaction", e);
                orderService.refundOrder(
                        AddOrUpdateOrderCommand.builder()
                                .correlationId(correlationId.toString())
                                .build());
                transactionsViews.somethingWentWrong(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
            }
        }
    }

    private void promptWalletDependingOnEnergyAmount(CallbackQuery callbackQuery, List<UserWallet> wallets,
            Integer energyAmount) {
        if (energyAmount == AppConstants.ENERGY_131K) {
            transactionsViews.updMenuToTransaction131kMenu(wallets, callbackQuery);
        } else {
            transactionsViews.updMenuToTransaction65kMenu(wallets, callbackQuery);
        }

    }
}
