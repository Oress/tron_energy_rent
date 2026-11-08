package org.ipan.nrgyrent.fixedfloat;

import jakarta.persistence.EntityManager;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:fixedfloat;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FixedFloatSettlementService.class, FixedFloatReviewNotificationService.class})
class FixedFloatPersistenceTest {
    @Autowired DepositTransactionRepo deposits;
    @Autowired BalanceRepo balances;
    @Autowired FixedFloatSettlementService settlement;
    @Autowired EntityManager em;
    @Autowired FixedFloatReviewNotificationService reviewNotifications;
    @Autowired FixedFloatReviewNotificationRepo notificationReceipts;
    @MockBean TelegramMessages messages;
    @MockBean TelegramState state;

    @Test void reviewDeliveryReceiptsSurviveReloadAndAreSeparateForEachRecipient() {
        DepositTransaction deposit = FixedFloatSettlementServiceTest.deposit();
        deposit.setId(null);
        deposit.setWalletFrom("sender");
        deposit.setTxId("review-usdt-tx");
        deposit.setAmount(0L);
        deposit.setStatus(DepositStatus.USDT_FF_REVIEW_REQUIRED);
        deposits.saveAndFlush(deposit);
        Long id = deposit.getId();
        UserState first = mock(UserState.class);
        UserState second = mock(UserState.class);
        when(state.getOrCreateUserState(99L)).thenReturn(first);
        when(state.getOrCreateUserState(100L)).thenReturn(second);

        reviewNotifications.notifyOnce(id, 99L);
        em.clear();
        reviewNotifications.notifyOnce(id, 99L);
        reviewNotifications.notifyOnce(id, 100L);
        em.clear();

        verify(messages).sendFixedFloatReviewNotification(eq(first), any());
        verify(messages).sendFixedFloatReviewNotification(eq(second), any());
        assertNotNull(notificationReceipts.findById(new FixedFloatReviewNotification.Id(id, 99L)).orElseThrow().getSentAt());
        assertNotNull(notificationReceipts.findById(new FixedFloatReviewNotification.Id(id, 100L)).orElseThrow().getSentAt());
    }

    @Test void settlementPersistsOnceAndIncrementsBalanceVersion() {
        Balance balance = new Balance();
        balance.setDepositAddress("user-wallet");
        balance.setSunBalance(1_000_000L);
        balances.saveAndFlush(balance);
        Long oldVersion = balance.getVersion();
        Long balanceId = balance.getId();

        DepositTransaction d = FixedFloatSettlementServiceTest.deposit();
        d.setId(null);
        d.setWalletFrom("sender");
        d.setTxId("original-usdt-tx");
        d.setAmount(0L);
        deposits.saveAndFlush(d);
        Long id = d.getId();
        em.clear();

        settlement.settle(id, FixedFloatSettlementServiceTest.order("DONE"));
        em.flush();
        em.clear();
        assertNull(settlement.settle(id, FixedFloatSettlementServiceTest.order("DONE")));
        assertEquals(24_904_938L, balances.findById(balanceId).orElseThrow().getSunBalance());
        assertEquals(oldVersion + 1, balances.findById(balanceId).orElseThrow().getVersion());
        assertEquals(DepositStatus.COMPLETED, deposits.findById(id).orElseThrow().getStatus());
    }

    @Test void transferClaimSucceedsOnlyOnce() {
        DepositTransaction d = FixedFloatSettlementServiceTest.deposit();
        d.setId(null);
        d.setWalletFrom("sender");
        d.setTxId("claim-usdt-tx");
        d.setAmount(0L);
        d.setStatus(DepositStatus.USDT_ENERGY_RENTED);
        deposits.saveAndFlush(d);
        assertEquals(1, deposits.transitionStatus(d.getId(), DepositStatus.USDT_ENERGY_RENTED, DepositStatus.USDT_FF_TRANSFERRING));
        assertEquals(0, deposits.transitionStatus(d.getId(), DepositStatus.USDT_ENERGY_RENTED, DepositStatus.USDT_FF_TRANSFERRING));
    }
}
