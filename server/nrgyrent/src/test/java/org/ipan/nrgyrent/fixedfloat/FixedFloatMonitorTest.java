package org.ipan.nrgyrent.fixedfloat;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.ipan.nrgyrent.fixedfloat.FixedFloatSettlementServiceTest.order;

class FixedFloatMonitorTest {
    private final FixedFloatClient client = mock(FixedFloatClient.class);
    private final DepositTransactionRepo deposits = mock(DepositTransactionRepo.class);
    private final FixedFloatSettlementService settlement = mock(FixedFloatSettlementService.class);
    private final BalanceRepo balances = mock(BalanceRepo.class);
    private final AppUserRepo users = mock(AppUserRepo.class);
    private final TelegramMessages messages = mock(TelegramMessages.class);
    private final FixedFloatReviewNotificationService reviews = mock(FixedFloatReviewNotificationService.class);
    private final FixedFloatMonitor monitor = new FixedFloatMonitor(client, deposits, settlement,
            balances, users, mock(TelegramState.class), messages, reviews);

    private DepositTransaction pending() {
        DepositTransaction d = FixedFloatSettlementServiceTest.deposit();
        when(deposits.findByExchangeProviderAndStatusIn(UsdtExchangeProvider.FIXEDFLOAT,
                FixedFloatMonitor.PENDING_STATUSES)).thenReturn(List.of(d));
        return d;
    }

    @Test void resumesPersistedOrdersAndSettlesOnlyCompletedPayouts() throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenReturn(order("DONE"));
        monitor.poll();
        verify(settlement).settle(42L, order("DONE"));
    }

    @ParameterizedTest @ValueSource(strings = {"NEW", "PENDING", "EXCHANGE", "WITHDRAW"})
    void keepsPollingUnfinishedOrders(String status) throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenReturn(order(status));
        monitor.poll();
        verifyNoInteractions(settlement);
        verifyNoInteractions(reviews);
        verify(deposits, never()).transitionStatus(anyLong(), any(), any());
    }

    @ParameterizedTest @ValueSource(strings = {"EXPIRED", "EMERGENCY"})
    void flagsProblemOrdersWithoutCreditingOrSendingAgain(String status) throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenReturn(order(status));
        monitor.poll();
        verifyNoInteractions(settlement);
        verify(deposits).transitionStatus(42L, DepositStatus.USDT_FF_IN_PROGRESS, DepositStatus.USDT_FF_REVIEW_REQUIRED);
    }

    @Test void transientFailureDoesNotDiscardPendingOrder() throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenThrow(new IOException("temporary failure"));
        monitor.poll();
        verifyNoInteractions(settlement);
        verify(deposits, never()).transitionStatus(anyLong(), any(), any());
        verifyNoInteractions(reviews);
    }

    @ParameterizedTest @ValueSource(strings = {"EXPIRED", "EMERGENCY", "UNKNOWN_PROBLEM"})
    void notifiesPersonalOwnerWhenProviderRequiresReview(String status) throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenReturn(order(status));
        when(deposits.transitionStatus(42L, DepositStatus.USDT_FF_IN_PROGRESS,
                DepositStatus.USDT_FF_REVIEW_REQUIRED)).thenReturn(1);
        Balance balance = new Balance();
        balance.setId(7L);
        when(balances.findByDepositAddress("user-wallet")).thenReturn(balance);
        when(users.findByBalanceId(7L)).thenReturn(user(99L));

        monitor.poll();

        verify(reviews).notifyOnce(42L, 99L);
        verifyNoInteractions(settlement, messages);
    }

    @Test void retriesExistingReviewForAllGroupMembersEvenWhenOneDeliveryFails() throws Exception {
        DepositTransaction deposit = pending();
        deposit.setStatus(DepositStatus.USDT_FF_REVIEW_REQUIRED);
        when(client.getOrder("ABC123", "token")).thenReturn(order("EMERGENCY"));
        Balance balance = new Balance();
        balance.setId(7L);
        balance.setType(BalanceType.GROUP);
        when(balances.findByDepositAddress("user-wallet")).thenReturn(balance);
        when(users.findAllByGroupBalanceId(7L)).thenReturn(Set.of(user(99L), user(100L)));
        doThrow(new RuntimeException("Telegram unavailable")).doNothing().when(reviews).notifyOnce(42L, 99L);

        monitor.poll();
        monitor.poll();

        verify(reviews, times(2)).notifyOnce(42L, 99L);
        verify(reviews, times(2)).notifyOnce(42L, 100L);
        verifyNoInteractions(settlement, messages);
    }

    @Test void localTransferUncertaintyDoesNotInventAProviderHold() throws Exception {
        pending().setStatus(DepositStatus.USDT_FF_REVIEW_REQUIRED);
        when(client.getOrder("ABC123", "token")).thenReturn(order("PENDING"));
        monitor.poll();
        verifyNoInteractions(reviews, messages);
    }

    @Test void lostStatusTransitionDoesNotSendAnAlert() throws Exception {
        pending();
        when(client.getOrder("ABC123", "token")).thenReturn(order("EMERGENCY"));
        monitor.poll();
        verifyNoInteractions(reviews, messages);
    }

    @Test void malformedOrderDoesNotSendAnAlert() throws Exception {
        pending().setExchangePayoutAddress("different-address");
        when(client.getOrder("ABC123", "token")).thenReturn(order("EMERGENCY"));
        monitor.poll();
        verifyNoInteractions(reviews, messages);
        verify(deposits, never()).transitionStatus(anyLong(), any(), any());
    }

    private AppUser user(Long telegramId) {
        AppUser user = new AppUser();
        user.setTelegramId(telegramId);
        return user;
    }
}
