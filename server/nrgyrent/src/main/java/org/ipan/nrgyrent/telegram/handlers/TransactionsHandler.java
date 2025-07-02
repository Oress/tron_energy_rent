package org.ipan.nrgyrent.telegram.handlers;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.UserWalletRepo;
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
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
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
    private final UserWalletRepo userWalletRepo;
    private final TelegramMessages telegramMessages;
    private final FormattingTools formattingTools;

    @MatchState(state = States.MAIN_MENU, updateTypes = UpdateType.CALLBACK_QUERY)
    public void startQuickTransaction(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        Long walletIdForQuickTx = InlineMenuCallbacks.getWalletIdForQuickTx(data);

        if (walletIdForQuickTx != null) {
            AppUser byId = userService.getById(userState.getTelegramId());
            Tariff tariff = byId.getTariffToUse();

            if (tariff == null) {
                logger.error("Quick TX. No tariff found during transaction for user: {}", userState.getTelegramId());
                transactionsViews.somethingWentWrong(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
                return;
            }
            
            UserWallet userWallet = userWalletRepo.findById(walletIdForQuickTx).orElse(null);
            if (userWallet == null) {
                logger.error("Quick TX. Could not find wallet {} for user: {}", walletIdForQuickTx, userState.getTelegramId());
                transactionsViews.somethingWentWrong(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
                return;
            }

            if (!userState.getTelegramId().equals(userWallet.getUser().getTelegramId())) {
                logger.error("Quick TX. Wallet {} does not belong to this user {}", walletIdForQuickTx, userState.getTelegramId());
                transactionsViews.somethingWentWrong(userState);
                telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_ERROR));
                return;
            }

            tryMakeTransaction(userState, AppConstants.ENERGY_65K, AppConstants.DURATION_1H, userWallet.getAddress(), 1, tariff.getTransactionType1AmountSun(), tariff.getId());
        }
    }


    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.CUSTOM_TRANSACTION_AMOUNT)
    public void startCustomTransactionAmount65K_promptAmount(UserState userState, Update update) {
        AppUser byId = userService.getById(userState.getTelegramId());
        Tariff tariff = byId.getTariffToUse();

        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());
        telegramState.updateTransactionParams(userState.getTelegramId(), 
        transactionParams
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
            promptWalletDependingOnEnergyAmount(userState, wallets, txAmount, AppConstants.ENERGY_65K);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PROMPT_WALLET));
        }
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_65k)
    public void handleTransaction65k(UserState userState, Update update) {
        proceedToSimpleTransaction(userState, update, AppConstants.ENERGY_65K);
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.TRANSACTION_131k)
    public void handleTransaction131k(UserState userState, Update update) {
        proceedToSimpleTransaction(userState, update, AppConstants.ENERGY_131K);
    }

    private void proceedToSimpleTransaction(UserState userState, Update update, Integer energyAmountPerTx) {
        TransactionParams transactionParams = telegramState.getOrCreateTransactionParams(userState.getTelegramId());

        telegramState.updateTransactionParams(userState.getTelegramId(),
            transactionParams
            .withNumberOfTransactions(1)
            .withEnergyAmount(energyAmountPerTx));

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.TRANSACTION_PROMPT_WALLET));
        List<UserWallet> wallets = userWalletService.getWallets(userState.getTelegramId());
        promptWalletDependingOnEnergyAmount(userState, wallets, 1, energyAmountPerTx);
    }

    @MatchStates({
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.CALLBACK_QUERY),
        @MatchState(state = States.TRANSACTION_PROMPT_WALLET, updateTypes = UpdateType.MESSAGE)
    })
    public void processWalletForTransaction(UserState userState, Update update) {
        TransactionParams transactionParams = telegramState
                .getOrCreateTransactionParams(userState.getTelegramId());

        AppUser byId = userService.getById(userState.getTelegramId());
        Integer energyAmountPerTx = transactionParams.getEnergyAmount();
        Tariff tariff = byId.getTariffToUse();

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
        Integer txAmount = transactionParams.getNumberOfTransactions();
        if (txAmount == null) {
            txAmount = 1;
        }
        handlePromptWallet(userState, update, energyAmountPerTx, txAmount, pricePerTx, tariff.getId());
    }

    private void handlePromptWallet(UserState userState, Update update, Integer energyAmountPerTx, Integer txAmount, Long sunAmountPerTx, Long tariffId) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery != null) {
            tryMakeTransaction(userState, energyAmountPerTx, AppConstants.DURATION_1H, callbackQuery.getData(), txAmount, sunAmountPerTx, tariffId);
        }

        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            telegramMessages.deleteMessage(message);
            tryMakeTransaction(userState, energyAmountPerTx, AppConstants.DURATION_1H, message.getText(), txAmount, sunAmountPerTx, tariffId);
        }
    }

    private void tryMakeTransaction(UserState userState, Integer energyAmountPerTx, String duration, String receiveAddress, Integer txAmount,
            Long sunAmountPerTx, Long tariffId) {
        if (WalletTools.isValidTronAddress(receiveAddress)) {
            transactionsViews.updMenuToTransactionInProgress(userState);

            UUID correlationId = UUID.randomUUID();
            Integer totalRentEnergy = energyAmountPerTx * txAmount;
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(totalRentEnergy, duration, receiveAddress);

            Order pendingOrder = null;
            try {
                pendingOrder = orderService.createPendingOrder(
                        AddOrUpdateOrderCommand.builder()
                                .userId(userState.getTelegramId())
                                .receiveAddress(receiveAddress)
                                .energyAmountPerTx(energyAmountPerTx)
                                .txAmount(txAmount)
                                .tariffId(tariffId)
                                .sunAmountPerTx(sunAmountPerTx)
                                .duration(duration)
                                .messageIdToUpdate(userState.getMenuMessageId())
                                .chatId(userState.getChatId())
                                .type(OrderType.USER)
                                .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                                .correlationId(correlationId.toString())
                                .build());

                AppUser user = userService.getById(userState.getTelegramId());
                transactionsViews.updMenuToTransactionPending(userState);

                List<UserWallet> userWallets = Collections.emptyList();
                if (user.getShowWalletsMenu()) {
                    userWallets = userWalletService.getWallets(user.getTelegramId());
                }
                
                Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
                telegramState.updateUserState(userState.getTelegramId(), userState
                        .withState(States.MAIN_MENU)
                        .withChatId(newMenuMsg.getChatId())
                        .withMenuMessageId(newMenuMsg.getMessageId()));

                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(totalRentEnergy, duration, receiveAddress,
                        correlationId);

                if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                    transactionsViews.somethingWentWrong(userState, pendingOrder);
                    return;
                }

            } catch (NotEnoughBalanceException e) {
                transactionsViews.notEnoughBalance(userState);
            } catch (InactiveAddressException e) {
                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                    transactionsViews.transactionToInactiveWallet(userState, pendingOrder);
                }
            } catch (ItrxInsufficientFundsException e) {
                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                    transactionsViews.itrxBalanceNotEnoughFunds(userState, pendingOrder);
                }
            } catch (Exception e) {
                logger.error("Error during transaction", e);

                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                    transactionsViews.somethingWentWrong(userState, pendingOrder);
                }
                transactionsViews.somethingWentWrong(userState);
            }
        }
    }

    private void promptWalletDependingOnEnergyAmount(UserState userState, List<UserWallet> wallets, Integer txAmount, Integer energyAmount) {
        AppUser byId = userService.getById(userState.getTelegramId());

        // NPE ?
        Tariff tariff = byId.getTariffToUse();

        if (tariff == null) {
            logger.error("User {} has no tariff set, cannot show transaction menu", formattingTools.formatUserForSearch(byId));
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
