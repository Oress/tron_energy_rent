package org.ipan.nrgyrent.fixedfloat;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixedFloatSettlementServiceTest {
    private final DepositTransactionRepo deposits = mock(DepositTransactionRepo.class);
    private final BalanceRepo balances = mock(BalanceRepo.class);
    private final FixedFloatSettlementService service = new FixedFloatSettlementService(deposits, balances);
    private DepositTransaction deposit;

    static DepositTransaction deposit() {
        DepositTransaction d = new DepositTransaction();
        d.setId(42L);
        d.setType(DepositType.USDT);
        d.setOriginalAmount(10_000_001L);
        d.setExchangeProvider(UsdtExchangeProvider.FIXEDFLOAT);
        d.setStatus(DepositStatus.USDT_FF_IN_PROGRESS);
        d.setExchangeOrderId("ABC123");
        d.setExchangeOrderToken("token");
        d.setExchangeSendAmount(10_000_001L);
        d.setExchangeUsdtTx("usdt-tx");
        d.setExchangeDepositAddress("THfnc8XaHUSaEZRPmGLP7qQCS8xn2EWVSE");
        d.setExchangePayoutAddress("TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf");
        d.setWalletTo("user-wallet");
        return d;
    }

    static FixedFloatOrder order(String status) {
        return order(status, new BigDecimal("10.000001"), new BigDecimal("28.12345678"));
    }

    static FixedFloatOrder order(String status, BigDecimal sentUsdt, BigDecimal paidTrx) {
        return new FixedFloatOrder("ABC123", "token", "float", status,
                new FixedFloatOrder.Currency("USDTTRC", "TRX", sentUsdt,
                        "THfnc8XaHUSaEZRPmGLP7qQCS8xn2EWVSE",
                        new FixedFloatOrder.Transaction("usdt-tx", sentUsdt)),
                new FixedFloatOrder.Currency("TRX", "TRX", new BigDecimal("30.0"),
                        "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf",
                        new FixedFloatOrder.Transaction("trx-tx", paidTrx)));
    }

    @BeforeEach void setup() {
        deposit = deposit();
        when(deposits.findLockedById(42L)).thenReturn(deposit);
        when(deposits.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void creditsActualPayoutLessBotFeeOnceWithoutDeductingProviderFeeAgain() {
        when(balances.creditDeposit("user-wallet", 23_904_938L)).thenReturn(1);
        assertSame(deposit, service.settle(42L, order("DONE")));
        assertEquals(23_904_938L, deposit.getAmount());
        assertEquals(new BigDecimal("0.355575"), deposit.getTrxToUsdtRate());
        assertEquals("trx-tx", deposit.getExchangeTrxTx());
        assertEquals(DepositStatus.COMPLETED, deposit.getStatus());
        assertNull(service.settle(42L, order("DONE")));
        verify(balances, times(1)).creditDeposit(anyString(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"10000000, 30, 25500000", "1000000000, 999.999999, 998499999", "10000000, 0.000002, 1"})
    void collectsBotFeeInTrxUsingActualRateAndRoundsOnlyFinalCredit(long original, BigDecimal payout, long expectedSun) {
        deposit.setOriginalAmount(original);
        deposit.setExchangeSendAmount(original);
        when(balances.creditDeposit("user-wallet", expectedSun)).thenReturn(1);
        service.settle(42L, order("DONE", BigDecimal.valueOf(original, 6), payout));
        assertEquals(expectedSun, deposit.getAmount());
        verify(balances).creditDeposit("user-wallet", expectedSun);
    }

    @Test void doesNotChargeFeeAgainForPreviouslyPreparedNetAmountOrder() {
        deposit.setExchangeSendAmount(8_500_001L);
        when(balances.creditDeposit("user-wallet", 28_123_456L)).thenReturn(1);
        service.settle(42L, order("DONE", new BigDecimal("8.500001"), new BigDecimal("28.12345678")));
        assertEquals(28_123_456L, deposit.getAmount());
        verify(balances).creditDeposit("user-wallet", 28_123_456L);
    }

    @ParameterizedTest @ValueSource(longs = {0, -1, 1_500_000, 1_499_999})
    void rejectsDepositsThatDoNotCoverTheBotFee(long original) {
        deposit.setOriginalAmount(original);
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order("DONE")));
        verifyNoInteractions(balances);
    }

    @ParameterizedTest @ValueSource(longs = {9_000_000, 10_000_002, 0, -1})
    void rejectsUnexpectedPersistedInputEvenWhenProviderReceivedIt(long sent) {
        deposit.setExchangeSendAmount(sent);
        assertThrows(IllegalStateException.class, () -> service.settle(42L,
                order("DONE", BigDecimal.valueOf(sent, 6), new BigDecimal("30"))));
        verifyNoInteractions(balances);
    }

    @Test void refusesCreditThatRoundsToZeroAfterBotFee() {
        assertThrows(IllegalStateException.class, () -> service.settle(42L,
                order("DONE", new BigDecimal("10.000001"), new BigDecimal("0.000001"))));
        verifyNoInteractions(balances);
    }

    @ParameterizedTest @ValueSource(strings = {"NEW", "PENDING", "EXCHANGE", "WITHDRAW", "EXPIRED", "EMERGENCY"})
    void neverCreditsUnfinishedOrProblemOrder(String status) {
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order(status)));
        verifyNoInteractions(balances);
    }

    @Test void rejectsMismatchedPayoutDestination() {
        deposit.setExchangePayoutAddress("wrong-wallet");
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order("DONE")));
        verifyNoInteractions(balances);
    }

    @Test void rejectsMismatchedIncomingTransaction() {
        deposit.setExchangeUsdtTx("different-tx");
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order("DONE")));
        verifyNoInteractions(balances);
    }

    @Test void rejectsDifferentReceivedAmount() {
        deposit.setExchangeSendAmount(9_000_000L);
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order("DONE")));
        verifyNoInteractions(balances);
    }

    @Test void missingBalanceDoesNotCompleteDeposit() {
        assertThrows(IllegalStateException.class, () -> service.settle(42L, order("DONE")));
        assertEquals(DepositStatus.USDT_FF_IN_PROGRESS, deposit.getStatus());
        verify(deposits, never()).save(any());
    }
}
