package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.UUID;

import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
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

    private final TariffRepo tariffRepo;
    private final TelegramState telegramState;
    private final TransactionsViews transactionsViews;
    private final ItrxService itrxService;
    private final UserService userService;
    private final OrderService orderService;
    private final UserWalletService userWalletService;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_65k)
    public void handleTransaction65k(UserState userState, Update update) {
        proceedToTransactions(userState, update, AppConstants.ENERGY_65K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_131k)
    public void handleTransaction131k(UserState userState, Update update) {
        proceedToTransactions(userState, update, AppConstants.ENERGY_131K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.CUSTOM_TRANSACTION_AMOUNT)
    public void startCustomTransactionAmount65K_promptAmount(UserState userState, Update update) {
        AppUser byId = userService.getById(userState.getTelegramId());
        boolean useGroupWallet = byId.getGroupBalance() != null;

        Tariff tariff = useGroupWallet ? 
            byId.getGroupBalance().getTariff() :
            byId.getBalance().getTariff();

        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
        telegramState.updateTransactionParams(userState.getTelegramId(), 
        transactionParams
            .withGroupBalance(useGroupWallet)
            .withEnergyAmount(AppConstants.ENERGY_65K));

        transactionsViews.updMenuToPromptTrxAmount(userState, tariff);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_CUSTOM_AMOUNT_65k));
    }


    @MatchState(state = States.TRANSACTION_CUSTOM_AMOUNT_65k, updateTypes = UpdateType.MESSAGE)
    public void handleCustomTransactionAmount65K_promptWallet(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            Integer txAmount = 1;
            try {
                txAmount = Integer.parseInt(message.getText());
                if (txAmount <= 0) {
                    logger.warn("User tries to enter the negative amount of transactions {}, userState {}", txAmount, userState);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Cannot parse the tx amount from text {}, userState {}", message.getText(), userState);
                return;
            }

            TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
            telegramState.updateTransactionParams(userState.getTelegramId(), transactionParams.withNumberOfTransactions(txAmount));
            List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
            promptWalletDependingOnEnergyAmount(userState, wallets, txAmount, AppConstants.ENERGY_65K, transactionParams.getGroupBalance());
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PROMPT_WALLET));
        }
    }

    private void proceedToTransactions(UserState userState, Update update, Integer energyAmountPerTx) {
        AppUser byId = userService.getById(userState.getTelegramId());
        // If no group balance, proceed to
        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
        boolean useGroupBalance = true;
        if (byId.getGroupBalance() == null) {
            useGroupBalance = false;
            telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams
                .withNumberOfTransactions(1)
                .withGroupBalance(useGroupBalance)
                .withEnergyAmount(energyAmountPerTx));
            selectPersonalBalanceTypeForTransaction(userState, update);
        } else {
            transactionsViews.updMenuToPromptBalanceType(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.TRANSACTION_PROMPT_BALANCE_TYPE));
        }
        telegramState.updateTransactionParams(userState.getTelegramId(),
                transactionParams
                .withNumberOfTransactions(1)
                .withGroupBalance(useGroupBalance)
                .withEnergyAmount(energyAmountPerTx));
    }

    @MatchStates({
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.CALLBACK_QUERY),
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.MESSAGE)
    })
    public void processWalletForTransaction(UserState userState, Update update) {
        TransactionParams transactionParams = telegramState
                .getOrCreateTransactionParams(userState.getTelegramId());

        Integer energyAmountPerTx = transactionParams.getEnergyAmount();
        Tariff tariff = transactionParams.getGroupBalance()
                ? tariffRepo.findGroupTariffByUserId(userState.getTelegramId())
                : tariffRepo.findIndividualTariffByUserId(userState.getTelegramId());

        if (tariff == null) {
            logger.error("No tariff found during transaction for user: {}, tr params: {}", userState.getTelegramId(), transactionParams);
            transactionsViews.somethingWentWrong(userState);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
            return;
        }

        Long pricePerTx = switch (energyAmountPerTx) {
            case AppConstants.ENERGY_65K -> tariff.getTransactionType1AmountSun();
            case AppConstants.ENERGY_131K -> tariff.getTransactionType2AmountSun();
            default -> {
                throw new IllegalStateException("Unexpected value: " + energyAmountPerTx);
            }
        };
        handlePromptWallet(userState, update, energyAmountPerTx, transactionParams.getNumberOfTransactions(), pricePerTx, transactionParams.getGroupBalance());
    }

    private void handlePromptWallet(UserState userState, Update update, Integer energyAmountPerTx, Integer txAmount, Long sunAmountPerTx,
            boolean useGroupWallet) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmountPerTx, AppConstants.DURATION_1H, callbackQuery.getData(), txAmount, sunAmountPerTx,
                    useGroupWallet);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            tryMakeTransaction(userState, energyAmountPerTx, AppConstants.DURATION_1H, message.getText(), txAmount, sunAmountPerTx,
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
        promptWalletDependingOnEnergyAmount(userState, wallets, 1, transactionParams.getEnergyAmount(),  true);
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
        promptWalletDependingOnEnergyAmount(userState, wallets, 1, transactionParams.getEnergyAmount(), false);
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmountPerTx, String duration, String walletAddress, Integer txAmount,
            Long sunAmountPerTx, boolean useGroupWallet) {
        if (WalletTools.isValidTronAddress(walletAddress)) {
            transactionsViews.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            Integer totalRentEnergy = energyAmountPerTx * txAmount;
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(totalRentEnergy, duration, walletAddress);

            Order pendingOrder = null;
            try {
                pendingOrder = orderService.createPendingOrder(
                        AddOrUpdateOrderCommand.builder()
                                .userId(userState.getTelegramId())
                                .useGroupWallet(useGroupWallet)
                                .receiveAddress(walletAddress)
                                .energyAmountPerTx(energyAmountPerTx)
                                .txAmount(txAmount)
                                .sunAmountPerTx(sunAmountPerTx)
                                .duration(duration)
                                .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                                .correlationId(correlationId.toString())
                                // .serial(placeOrderResponse.getSerial())
                                .build());

                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(totalRentEnergy, duration, walletAddress,
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
            } catch (NotEnoughBalanceException e) {
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

                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
                transactionsViews.somethingWentWrong(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
            }
        }
    }

    private void promptWalletDependingOnEnergyAmount(UserState userState, List<UserWallet> wallets, Integer txAmount,
            Integer energyAmount, boolean useGroupWallet) {
        AppUser byId = userService.getById(userState.getTelegramId());

        // NPE ?
        Tariff tariff = useGroupWallet ? 
                byId.getGroupBalance().getTariff() :
                byId.getBalance().getTariff();

        if (tariff == null) {
            logger.error("User {} has no tariff set, cannot show transaction menu", byId.getTelegramUsername());
            transactionsViews.somethingWentWrong(userState);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
            return;
        }

        if (txAmount != 1) {
            transactionsViews.updMenuToCustomAmounTransaction65kMenu(wallets, userState, txAmount, tariff);
        } else if (energyAmount == AppConstants.ENERGY_131K) {
            transactionsViews.updMenuToTransaction131kMenu(wallets, userState, tariff);
        } else if (energyAmount == AppConstants.ENERGY_65K) {
            transactionsViews.updMenuToTransaction65kMenu(wallets, userState, tariff);
        } else {
            logger.error("Unknown transaction request energyAmount: {}, txAmount: {}, userstate: {}", energyAmount, txAmount, userState);
        }

    }
}
