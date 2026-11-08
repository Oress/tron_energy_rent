package org.ipan.nrgyrent.cron;

import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.domain.service.UsdtExchangeSettingsService;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsdtDepositOrchestratorTest {
    private final UsdtDepositHelper helper = mock(UsdtDepositHelper.class);
    private final DepositTransactionRepo deposits = mock(DepositTransactionRepo.class);
    private final UsdtExchangeSettingsService settings = mock(UsdtExchangeSettingsService.class);
    private final UsdtExchangeStrategy bybit = mock(UsdtExchangeStrategy.class);
    private final UsdtExchangeStrategy ff = mock(UsdtExchangeStrategy.class);
    private final UsdtDepositConfig config = new UsdtDepositConfig();
    private final TelegramMessages messages = mock(TelegramMessages.class);
    private final UsdtDepositOrchestrator orchestrator = new UsdtDepositOrchestrator(helper, deposits,
            List.of(bybit, ff), config, settings, messages, 123L);
    private DepositTransaction deposit;

    @BeforeEach void setup() {
        deposit = new DepositTransaction();
        deposit.setId(42L);
        deposit.setType(DepositType.USDT);
        deposit.setStatus(DepositStatus.USDT_ACKNOWLEDGED);
        when(deposits.findById(42L)).thenReturn(Optional.of(deposit));
        when(settings.currentProvider()).thenReturn(UsdtExchangeProvider.FIXEDFLOAT);
        when(bybit.provider()).thenReturn(UsdtExchangeProvider.BYBIT);
        when(ff.provider()).thenReturn(UsdtExchangeProvider.FIXEDFLOAT);
        when(deposits.transitionStatus(42L, DepositStatus.USDT_ACKNOWLEDGED,
                DepositStatus.USDT_EXCHANGE_PREPARING)).thenReturn(1);
    }

    @Test void disabledTopupsDoNotPrepareOrTransfer() {
        config.setEnabled(false);
        orchestrator.startOrchestrateUsdtDeposit(42L);
        assertEquals(DepositStatus.USDT_TOPUP_DISABLED, deposit.getStatus());
        verify(messages).sendUsdtTopupDisabledAdmin(123L, deposit);
        verify(ff, never()).prepare(any());
        verifyNoInteractions(helper);
    }

    @Test void failedPreparationNeverActivatesWalletOrRentsEnergy() {
        when(ff.prepare(deposit)).thenThrow(new IllegalStateException("unavailable"));
        orchestrator.startOrchestrateUsdtDeposit(42L);
        assertEquals(UsdtExchangeProvider.FIXEDFLOAT, deposit.getExchangeProvider());
        assertEquals(DepositStatus.USDT_EXCHANGE_PREPARATION_FAILED, deposit.getStatus());
        verify(bybit, never()).prepare(any());
        verifyNoInteractions(helper);
    }

    @Test void duplicateStartCannotCreateAnotherOrder() {
        when(deposits.transitionStatus(42L, DepositStatus.USDT_ACKNOWLEDGED,
                DepositStatus.USDT_EXCHANGE_PREPARING)).thenReturn(0);
        orchestrator.startOrchestrateUsdtDeposit(42L);
        verify(ff, never()).prepare(any());
        verifyNoInteractions(helper);
    }

    @Test void usesPersistedAdminSelectionAndProviderDestinationForEnergy() {
        when(ff.prepare(deposit)).thenReturn("ff-destination");
        orchestrator.startOrchestrateUsdtDeposit(42L);
        assertEquals(UsdtExchangeProvider.FIXEDFLOAT, deposit.getExchangeProvider());
        verify(helper).rentEnergyForUsdtTransfer(deposit, "ff-destination");
        verify(bybit, never()).prepare(any());
    }
}
