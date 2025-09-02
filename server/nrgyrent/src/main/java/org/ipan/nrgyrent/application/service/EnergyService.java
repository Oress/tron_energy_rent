package org.ipan.nrgyrent.application.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.events.autotopup.AutoDelegationSessionEventPublisher;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEventType;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSessionStatus;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.service.AutoDelegationSessionService;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.InactiveAddressException;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.tron.node.api.FullNodeRestClient;
import org.ipan.nrgyrent.tron.node.api.dto.AccountResource;
import org.ipan.nrgyrent.tron.node.events.ContractTypes;
import org.ipan.nrgyrent.tron.node.events.dto.AddressTransactionEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class EnergyService {
    private static final int ITRX_OK_CODE = 0;

    private final TelegramState telegramState;
    private final ItrxService itrxService;
    private final RestClient itrxRestClient;
    private final RestClient trxxRestClient;
    private final OrderService orderService;

    private final AutoDelegationSessionRepo autoTopupConfigRepo;
    private final FullNodeRestClient fullNodeRestClient;
    private final AutoDelegationSessionService autoDelegationSessionService;
    private final AutoDelegationSessionEventPublisher autoDelegationSessionEventPublisher;

    public EnergyService(TelegramState telegramState,
                         ItrxService itrxService,
                         RestClient itrxRestClient,
                         @Qualifier(AppConstants.TRXX_REST_CLIENT) RestClient trxxRestClient,
                         OrderService orderService,
                         AutoDelegationSessionRepo autoTopupConfigRepo,
                         FullNodeRestClient fullNodeRestClient,
                         AutoDelegationSessionService autoDelegationSessionService,
                         AutoDelegationSessionEventPublisher autoDelegationSessionEventPublisher) {
        this.telegramState = telegramState;
        this.itrxService = itrxService;
        this.itrxRestClient = itrxRestClient;
        this.trxxRestClient = trxxRestClient;
        this.orderService = orderService;
        this.autoTopupConfigRepo = autoTopupConfigRepo;
        this.fullNodeRestClient = fullNodeRestClient;
        this.autoDelegationSessionService = autoDelegationSessionService;
        this.autoDelegationSessionEventPublisher = autoDelegationSessionEventPublisher;
    }

    @Transactional
    public AutoDelegationSession startAutoTopupSession(UserState userState, String wallet) {
        logger.info("AUTO DELEGATION. Initiating auto delegation session for a userId {} wallet {} ", userState.getTelegramId(),  wallet);
        AutoDelegationSession newSession = null;
        // 0. check whether this wallet already has active autotopup config. no matter the user.
        // if there is, then show error.
        // 1. create a session and save it to DB.
        newSession = autoDelegationSessionService.createSession(
                userState.getTelegramId(),
                userState.getMenuMessageId(),
                userState.getChatId(),
                wallet
        );
        // 2. save session to cache
//        telegramState.createWalletMonitoringState(newSession.getId(), wallet);
        return newSession;
    }

    public AutoDelegationSession deactivateSessionLowBalance(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (low balance) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        byId = deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_INSUFFICIENT_BALANCE);
        return byId;
    }

    public AutoDelegationSession deactivateSessionInactivity(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (inactivity) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        byId = deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_INACTIVITY);
        return byId;
    }

    public AutoDelegationSession deactivateSessionManually(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (manually) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        byId = deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_BY_USER);
        return byId;
    }

    public void deactivateSessionSystemRestart(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (restart) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_SYSTEM_RESTART);
    }

    public void deactivateSessionNodeDisconnected(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (restart) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_NODE_DISCONNECTED);
    }

    public void deactivateSessionInitProblem(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (Initialization problem) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_INIT_PROBLEM);
    }

    public void deactivateSessionInactiveWallet(Long sessionId) {
        logger.info("AUTO DELEGATION. Deactivating auto delegation (Initialization problem) session id {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        deactivateSession(byId, AutoDelegationSessionStatus.STOPPED_INACTIVE_WALLET);
    }

    @Async
    public void tryMakeFirstAutoTopupAsync(Long sessionId) {
        logger.info("AUTO DELEGATION. Initiating first delegation for a sessionId {} ", sessionId);
        AutoDelegationSession byId = autoTopupConfigRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not found by id"));
        AccountResource accountResource = fullNodeRestClient.getAccountResource(byId.getAddress());
        Order order = autoDelegate(byId, accountResource, AutoDelegationEventType.INIT_DELEGATION);
        if (order != null) {
            logger.info("AUTO DELEGATION. First delegation has been made order id {}  session id: {}", order.getId(), sessionId);
            autoDelegationSessionEventPublisher.publishSuccessfulDelegationEvent(sessionId, order.getId());
        }
    }

    private Order autoDelegate(AutoDelegationSession delegationSession, AccountResource accountResource, AutoDelegationEventType delegationType) {
        Order order = null;
        Integer energyMax = accountResource.getEnergyLimit();
        Integer energyUsed = accountResource.getEnergyUsed();

        String wallet = delegationSession.getAddress();

        AppUser user = delegationSession.getUser();
        Tariff tariff = user.getTariffToUse();
        UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());

        int energyAmount;
        long priceSun;
        // This is possible when wallet has energy but not used it yet
        energyUsed = energyUsed == null ? 0 : energyUsed;
        energyMax = energyMax == null ? 0 : energyMax;
        int availableEnergy = energyMax - energyUsed;
        // Scenario 1: Initial topup
        if (availableEnergy < AppConstants.ENERGY_65K) {
            // topup 131
            // update msg and last SC ts
            energyAmount = AppConstants.ENERGY_131K;
            priceSun = tariff.getTransactionType2AmountSun();
        } else if (availableEnergy < AppConstants.ENERGY_131K) {
            // topup 65
            // update msg and last SC ts
            energyAmount = AppConstants.ENERGY_65K;
            priceSun = tariff.getTransactionType1AmountSun();
        } else {
            logger.info("Wallet available energy is > 131k {}", availableEnergy);
            return null;
        }
        try {
            order = tryMakeTransaction(userState, energyAmount, AppConstants.DURATION_1H, wallet, 1, priceSun, tariff.getId(),
                    delegationSession.getId(), delegationType);
            logger.info("AUTO DELEGATION. perform delegation energy {} price {} order id {} session id: {}", energyAmount, priceSun, order.getId(), delegationSession.getId());
        } catch (NotEnoughBalanceException e) {
            logger.warn("AUTO DELEGATION. Not enough balance, stopping session. session id {} ", delegationSession.getId());
            deactivateSession(delegationSession, AutoDelegationSessionStatus.STOPPED_INSUFFICIENT_BALANCE);
        } catch (InactiveAddressException e) {
            logger.warn("AUTO DELEGATION. Inactive wallet stopping session. session id {} ", delegationSession.getId());
            deactivateSession(delegationSession, AutoDelegationSessionStatus.STOPPED_INACTIVE_WALLET);
        } catch (Exception e) {
            logger.error("AUTO DELEGATION. Something went wrong stopping session. session id {} ", delegationSession.getId());
            deactivateSession(delegationSession, AutoDelegationSessionStatus.STOPPED_ERROR);
        }
        return order;
    }

    private AutoDelegationSession deactivateSession(AutoDelegationSession session, AutoDelegationSessionStatus status) {
        AutoDelegationSession removedSession = autoDelegationSessionService.deactivate(session.getId(), status);

        if (removedSession.getEnergyProvider() == EnergyProviderName.TRXX) {
            trxxRestClient.editDelegatePolicy(session.getAddress(), true);
        } else {
            itrxRestClient.editDelegatePolicy(session.getAddress(), true);
        }

        return removedSession;
    }

    public Order tryMakeSystemTransaction(Integer energyAmount, String duration, String receiveAddress) {
        Order pendingOrder = null;

        if (WalletTools.isValidTronAddress(receiveAddress)) {
            UUID correlationId = UUID.randomUUID();
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(energyAmount, duration, receiveAddress);

            try {
                var builder = AddOrUpdateOrderCommand.builder()
                        .receiveAddress(receiveAddress)
                        .energyAmountPerTx(estimateOrderResponse.getEnergy_amount())
                        .txAmount(1)
                        .duration(duration)
                        .type(OrderType.WITHDRAW_TRX_TO_BYBIT)
                        .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                        .correlationId(correlationId.toString());
                pendingOrder = orderService.createPendingOrder(builder.build());
                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(estimateOrderResponse.getEnergy_amount(), duration, receiveAddress,
                        correlationId);

                if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
            } catch (Exception e) {
                logger.error("Error while placing order ", e);
                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
                throw e;
            }
        }
        return pendingOrder;
    }

    public Order tryMakeTransaction(UserState userState, Integer energyAmountPerTx, String duration, String receiveAddress, Integer txAmount,
            Long sunAmountPerTx, Long tariffId, Long autoDelegationSessionId, AutoDelegationEventType delegationEventType) {
        Order pendingOrder = null;

        if (WalletTools.isValidTronAddress(receiveAddress)) {
            UUID correlationId = UUID.randomUUID();
            Integer totalRentEnergy = energyAmountPerTx * txAmount;
            EstimateOrderAmountResponse estimateOrderResponse = itrxService.estimateOrderPrice(totalRentEnergy, duration, receiveAddress);

            try {
                var builder = AddOrUpdateOrderCommand.builder()
                        .userId(userState.getTelegramId())
                        .receiveAddress(receiveAddress)
                        .energyAmountPerTx(energyAmountPerTx)
                        .txAmount(txAmount)
                        .tariffId(tariffId)
                        .type(OrderType.USER)
                        .sunAmountPerTx(sunAmountPerTx)
                        .duration(duration)
                        .itrxFeeSunAmount(estimateOrderResponse.getTotal_price())
                        .correlationId(correlationId.toString());
                if (autoDelegationSessionId != null) {
                    builder
                            .autoDelegationSessionId(autoDelegationSessionId)
                            .delegationEventType(delegationEventType);
                } else {
                    builder
                            .messageIdToUpdate(userState.getMenuMessageId())
                            .chatId(userState.getChatId());
                }
                pendingOrder = orderService.createPendingOrder(builder.build());
                PlaceOrderResponse placeOrderResponse = itrxService.placeOrder(totalRentEnergy, duration, receiveAddress,
                        correlationId);

                if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
            } catch (Exception e) {
                logger.error("Error while placing order ", e);
                if (pendingOrder != null) {
                    orderService.refundOrder(
                            AddOrUpdateOrderCommand.builder()
                                    .correlationId(correlationId.toString())
                                    .build());
                }
                throw e;
            }
        }
        return pendingOrder;
    }

    @Async
    public void processTxEventAsync(AddressTransactionEvent event) {
        String wallet = switch (event.getContractType()) {
            case ContractTypes.UNDELEGATE_RESOURCE -> event.getToAddress();
            default -> event.getFromAddress();
        };
        List<AutoDelegationSession> byWalletAndActive = autoTopupConfigRepo.findByAddressAndActive(wallet, Boolean.TRUE);

        if (byWalletAndActive.size() != 1) {
            logger.error("AUTO DELEGATION. 0 or more than 1 active topup configurations for the wallet {}", wallet);
            return;
        }

        AutoDelegationSession autotopupSession = byWalletAndActive.get(0);
        if (ContractTypes.TRIGGER_SC.equals(event.getContractType())) {
            AccountResource accountResource = fullNodeRestClient.getAccountResource(wallet);
            Order order = autoDelegate(autotopupSession, accountResource, AutoDelegationEventType.SC_INVOCATION);
            if (order != null) {
                logger.info("AUTO DELEGATION. trgiggered by SC Invocation. order id {}  session id: {}", order.getId(), autotopupSession.getId());
                autoDelegationSessionEventPublisher.publishSuccessfulDelegationEvent(autotopupSession.getId(), order.getId());
                autoDelegationSessionService.updateLastSmartContractTs(autotopupSession.getId(), event.getTimeStamp());
            }
        } else if (ContractTypes.UNDELEGATE_RESOURCE.equals(event.getContractType())) {
            AccountResource accountResource = fullNodeRestClient.getAccountResource(wallet);

            Integer energyMax = accountResource.getEnergyLimit();
            Integer energyUsed = accountResource.getEnergyUsed();
            if (energyMax == null || energyUsed == null) {
                logger.info("AUTO DELEGATION. Undelegated all energy, disabling config. config id {} wallet {}, response, {}", autotopupSession.getId(), wallet, accountResource);
                deactivateSession(autotopupSession, AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED);
                return;
            }

            Long lastSmartContractTs = autotopupSession.getLastSmartContractTs();
            // means that we get undelegate and the energy was not used.
            if (lastSmartContractTs == null) {
                logger.info("AUTO DELEGATION. Undelegate energy, It was not used for SC, disabling config. config id {} wallet {}, response, {}", autotopupSession.getId(), wallet, accountResource);
                deactivateSession(autotopupSession, AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED);
            } else if (System.currentTimeMillis() - lastSmartContractTs >= AppConstants.HOUR_MILLIS) {
                logger.info("AUTO DELEGATION. Undelegate energy, Last SC invocation > 1H disabling config. config id {} wallet {}, response, {}", autotopupSession.getId(), wallet, accountResource);
                deactivateSession(autotopupSession, AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED);
            } else if (System.currentTimeMillis() - lastSmartContractTs < AppConstants.HOUR_MILLIS) {
                Order order = autoDelegate(autotopupSession, accountResource, AutoDelegationEventType.REDELEGATE_PARTIALLY);
                if (order != null) {
                    logger.info("AUTO DELEGATION. Refill time from last SC invocation is less than 1H has been made order id {}  session id: {}", order.getId(), autotopupSession.getId());
                    autoDelegationSessionEventPublisher.publishSuccessfulDelegationEvent(autotopupSession.getId(), order.getId());
                }
            }
        } else {
            logger.warn("Unexpected type of tx event, {}", event);
        }
    }
}
