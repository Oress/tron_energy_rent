package org.ipan.nrgyrent.fixedfloat;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.domain.service.ManagedWalletService;
import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.ipan.nrgyrent.fixedfloat.FixedFloatSettlementServiceTest.order;

class FixedFloatUsdtExchangeStrategyTest {
    private final FixedFloatClient client = mock(FixedFloatClient.class);
    private final DepositTransactionRepo deposits = mock(DepositTransactionRepo.class);
    private final CollectionWalletRepo collectionWallets = mock(CollectionWalletRepo.class);
    private final ManagedWalletRepo wallets = mock(ManagedWalletRepo.class);
    private final ManagedWalletService signing = mock(ManagedWalletService.class);
    private final TrongridRestClient tron = mock(TrongridRestClient.class);
    private final FixedFloatUsdtExchangeStrategy strategy = new FixedFloatUsdtExchangeStrategy(
            client, deposits, collectionWallets, wallets, signing, tron);
    private DepositTransaction deposit;

    @BeforeEach void setup() {
        deposit = FixedFloatSettlementServiceTest.deposit();
    }

    @Test void preparesFullDepositWithoutRetainingBotFeeOrOneCent() throws Exception {
        deposit.setExchangeOrderId(null);
        CollectionWallet collection = new CollectionWallet();
        collection.setWalletAddress(deposit.getExchangePayoutAddress());
        when(collectionWallets.findFirstByIsActive(true)).thenReturn(collection);
        when(client.createOrder(new BigDecimal("10.000001"), collection.getWalletAddress())).thenReturn(order("NEW"));
        assertEquals(deposit.getExchangeDepositAddress(), strategy.prepare(deposit));
        assertEquals(deposit.getOriginalAmount(), deposit.getExchangeSendAmount());
        assertEquals("ABC123", deposit.getExchangeOrderId());
        assertEquals("token", deposit.getExchangeOrderToken());
        verifyNoInteractions(tron);
    }

    @ParameterizedTest @ValueSource(longs = {0, -1, 1_500_000, 1_499_999})
    void refusesDepositsThatCannotCoverBotFeeBeforeCreatingOrder(long amount) {
        deposit.setExchangeOrderId(null);
        deposit.setOriginalAmount(amount);
        CollectionWallet collection = new CollectionWallet();
        collection.setWalletAddress(deposit.getExchangePayoutAddress());
        when(collectionWallets.findFirstByIsActive(true)).thenReturn(collection);
        assertThrows(IllegalArgumentException.class, () -> strategy.prepare(deposit));
        verifyNoInteractions(client, tron);
    }

    @Test void repeatedCallbackCannotTransferAgain() {
        strategy.exchange(deposit);
        verifyNoInteractions(client, tron, signing);
    }

    private void prepareTransfer(String status) throws Exception {
        when(deposits.transitionStatus(42L, DepositStatus.USDT_ENERGY_RENTED,
                DepositStatus.USDT_FF_TRANSFERRING)).thenReturn(1);
        when(client.getOrder("ABC123", "token")).thenReturn(order(status));
    }

    @Test void doesNotFundExpiredOrder() throws Exception {
        prepareTransfer("EXPIRED");
        strategy.exchange(deposit);
        verifyNoInteractions(tron);
        verify(deposits).transitionStatus(42L, DepositStatus.USDT_FF_TRANSFERRING,
                DepositStatus.USDT_FF_REVIEW_REQUIRED);
    }

    @Test void persistsTransactionBeforeBroadcastAndThenWaitsForSettlement() throws Exception {
        prepareTransfer("NEW");
        stubTransfer();
        when(tron.broadcastTransaction(any())).thenReturn(new TreeMap<>(Map.of("result", true, "txid", "usdt-tx")));
        strategy.exchange(deposit);
        var sequence = inOrder(deposits, tron);
        sequence.verify(deposits).transitionStatus(42L, DepositStatus.USDT_ENERGY_RENTED, DepositStatus.USDT_FF_TRANSFERRING);
        sequence.verify(tron).transferUsdtSmartContract("user-wallet", deposit.getExchangeDepositAddress(), 10_000_001L);
        sequence.verify(deposits).saveAndFlush(deposit);
        sequence.verify(tron).broadcastTransaction(any());
        sequence.verify(deposits).transitionStatus(42L, DepositStatus.USDT_FF_TRANSFERRING, DepositStatus.USDT_FF_IN_PROGRESS);
        assertEquals("usdt-tx", deposit.getExchangeUsdtTx());
        assertNull(deposit.getAmount());
    }

    @Test void ambiguousBroadcastIsNotRetried() throws Exception {
        prepareTransfer("NEW");
        stubTransfer();
        when(tron.broadcastTransaction(any())).thenThrow(new IllegalStateException("timeout"));
        strategy.exchange(deposit);
        verify(tron, times(1)).broadcastTransaction(any());
        verify(deposits).transitionStatus(42L, DepositStatus.USDT_FF_TRANSFERRING, DepositStatus.USDT_FF_REVIEW_REQUIRED);
        assertEquals("usdt-tx", deposit.getExchangeUsdtTx());
    }

    private void stubTransfer() {
        when(tron.transferUsdtSmartContract("user-wallet", deposit.getExchangeDepositAddress(), deposit.getExchangeSendAmount()))
                .thenReturn(new TreeMap<>(Map.of("transaction", Map.of("txID", "usdt-tx"))));
        ManagedWallet wallet = new ManagedWallet();
        when(wallets.findById("user-wallet")).thenReturn(Optional.of(wallet));
        when(signing.sign(wallet, "usdt-tx")).thenReturn("signature");
    }
}
