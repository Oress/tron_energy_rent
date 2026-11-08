package org.ipan.nrgyrent.fixedfloat;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixedFloatReviewNotificationServiceTest {
    private final DepositTransactionRepo deposits = mock(DepositTransactionRepo.class);
    private final FixedFloatReviewNotificationRepo notifications = mock(FixedFloatReviewNotificationRepo.class);
    private final TelegramState state = mock(TelegramState.class);
    private final TelegramMessages messages = mock(TelegramMessages.class);
    private final FixedFloatReviewNotificationService service =
            new FixedFloatReviewNotificationService(deposits, notifications, state, messages);

    @Test void retriesFailedDeliveryWithoutMarkingItDelivered() {
        DepositTransaction deposit = FixedFloatSettlementServiceTest.deposit();
        deposit.setStatus(DepositStatus.USDT_FF_REVIEW_REQUIRED);
        when(deposits.findLockedById(42L)).thenReturn(deposit);
        UserState user = mock(UserState.class);
        when(state.getOrCreateUserState(99L)).thenReturn(user);
        doThrow(new RuntimeException("Telegram unavailable")).doNothing()
                .when(messages).sendFixedFloatReviewNotification(user, deposit);

        assertThrows(RuntimeException.class, () -> service.notifyOnce(42L, 99L));
        verify(notifications, never()).saveAndFlush(any());
        service.notifyOnce(42L, 99L);

        verify(messages, times(2)).sendFixedFloatReviewNotification(user, deposit);
        var sent = org.mockito.ArgumentCaptor.forClass(FixedFloatReviewNotification.class);
        verify(notifications).saveAndFlush(sent.capture());
        assertEquals(new FixedFloatReviewNotification.Id(42L, 99L), sent.getValue().getId());
        assertNotNull(sent.getValue().getSentAt());
    }

    @ParameterizedTest
    @EnumSource(value = DepositStatus.class, names = {"COMPLETED", "USDT_FF_IN_PROGRESS"})
    void doesNotSendAfterSettlementOrWhenNotInReview(DepositStatus status) {
        DepositTransaction deposit = FixedFloatSettlementServiceTest.deposit();
        deposit.setStatus(status);
        when(deposits.findLockedById(42L)).thenReturn(deposit);
        service.notifyOnce(42L, 99L);
        verifyNoInteractions(notifications, messages, state);
    }

    @Test void previouslyNotifiedRecipientIsSkipped() {
        DepositTransaction deposit = FixedFloatSettlementServiceTest.deposit();
        deposit.setStatus(DepositStatus.USDT_FF_REVIEW_REQUIRED);
        when(deposits.findLockedById(42L)).thenReturn(deposit);
        when(notifications.existsById(new FixedFloatReviewNotification.Id(42L, 99L))).thenReturn(true);
        service.notifyOnce(42L, 99L);
        verifyNoInteractions(messages, state);
    }
}
