package org.ipan.nrgyrent.application.events;

import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.cron.UsdtDepositOrchestrator;
import org.ipan.nrgyrent.domain.events.OrderCompletedEvent;
import org.ipan.nrgyrent.domain.events.OrderFailedEvent;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class OrderEventApplicationListener {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final OrderRepo orderRepo;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final AutoDelegationViews autoDelegationViews;
    private final DepositTransactionRepo depositTransactionRepo;
    private final UsdtDepositOrchestrator usdtDepositOrchestrator;
    private final UserWalletService userWalletService;
    private final EnergyService energyService;

    @EventListener
    @org.springframework.core.annotation.Order(10)
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);

        Order order = orderRepo.findByCorrelationId(event.getCorrelationId()).orElse(null);
        if (order == null) {
            logger.error("Order not found for correlationId: {}", event.getCorrelationId());
            return;
        }
        AppUser user = order.getUser();
        if (event.getIsAutoDelegation()) {
            UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
            autoDelegationViews.sendAutoDelegationTransactionNotification(userState, order);

            Balance balance = order.getBalance();
            Tariff tariffToUse = order.getTariff();
            if (balance.getSunBalance() < tariffToUse.getMaxAutodelegateFee()) {
                AutoDelegationSession autoDelegationSession = order.getAutoDelegationSession();
                autoDelegationSession = energyService.deactivateSessionLowBalance(autoDelegationSession.getId());
                autoDelegationViews.autoDelegateSessionStoppedLowBalance(userState, autoDelegationSession);
            }

            List<UserWallet> userWallets = Collections.emptyList();
            if (user.getShowWalletsMenu()) {
                userWallets = userWalletService.getWallets(user.getTelegramId());
            }
            Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
            telegramState.updateUserState(userState.getTelegramId(), userState
                    .withState(States.MAIN_MENU)
                    .withChatId(newMenuMsg.getChatId())
                    .withMenuMessageId(newMenuMsg.getMessageId()));
        } else {
            if (OrderType.WITHDRAW_TRX_TO_BYBIT.equals(order.getType())) {
                usdtDepositOrchestrator.continueOrchestrateUsdtDepositWithOrderId(order.getCorrelationId());
            } else {
                UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                telegramMessages.sendTransactionSuccessNotification(userState, order);
            }
        }

    }

    @EventListener
    @Transactional
    @org.springframework.core.annotation.Order(10)
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);

        Order order = orderRepo.findByCorrelationId(event.getCorrelationId()).orElse(null);
        if (order == null) {
            logger.error("Order not found for correlationId: {}", event.getCorrelationId());
            return;
        }

        if (OrderType.WITHDRAW_TRX_TO_BYBIT.equals(order.getType())) {
            logger.error("Order failed for withdraw TRX to Bybit: {}", order);
            DepositTransaction depositTransaction = depositTransactionRepo.findBySystemOrder(order);
            if (depositTransaction != null) {
                depositTransaction.setStatus(DepositStatus.USDT_ENERGY_RENT_FAILED);
            }
        }
/*
        else if (order.getMessageToUpdate() == null) {
            UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
            logger.info("Order has no message id (AUTODELEGATION): correlationId {}", event.getCorrelationId());
            AutoDelegationSession session = autoDelegationSessionRepo.findSessionByOrderId(order.getId());
            autoDelegationViews.updateSessionStatus(userState, session);
        } else {
            UserState userState = telegramState.getOrCreateUserState(order.getUser().getTelegramId());
            telegramMessages.sendTransactionRefundNotification(userState, order);
        }
*/

    }
}
