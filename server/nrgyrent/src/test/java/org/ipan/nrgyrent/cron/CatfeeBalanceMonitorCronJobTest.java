package org.ipan.nrgyrent.cron;

import org.ipan.nrgyrent.catfee.CatfeeRestClient;
import org.ipan.nrgyrent.catfee.dto.CfAccountResponse;
import org.ipan.nrgyrent.catfee.dto.CfResponse;
import org.ipan.nrgyrent.domain.events.BalanceEventDomainListener;
import org.ipan.nrgyrent.domain.events.BalanceUpdatedEvent;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.domain.model.Alert;
import org.ipan.nrgyrent.domain.model.AlertStatus;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.ItrxBalance;
import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.ItrxBalanceRepository;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatfeeBalanceMonitorCronJobTest {
    private static final long THRESHOLD = 800_000_000L;

    @Mock
    private CatfeeRestClient restClient;
    @Mock
    private ItrxBalanceRepository balanceRepository;
    @Mock
    private AlertRepo alertRepo;
    @Mock
    private AlertService alertService;
    @Mock
    private AppUserRepo userRepo;
    @Mock
    private TelegramState telegramState;
    @Mock
    private TelegramMessages telegramMessages;

    private CatfeeBalanceMonitorCronJob monitor;

    @BeforeEach
    void setUp() {
        BalanceEventDomainListener listener = new BalanceEventDomainListener(
                balanceRepository, alertRepo, alertService, userRepo, telegramState, telegramMessages);
        ReflectionTestUtils.setField(listener, "balanceThreshold", THRESHOLD);
        OrderEventPublisher eventPublisher = new OrderEventPublisher(event -> {
            BalanceUpdatedEvent balanceEvent = assertInstanceOf(BalanceUpdatedEvent.class, event);
            assertEquals(EnergyProviderName.CATFEE, balanceEvent.getEnergyProviderName());
            listener.onBalanceUpdated(balanceEvent);
        });
        monitor = new CatfeeBalanceMonitorCronJob(restClient, eventPublisher);
    }

    @Test
    void savesHealthyBalanceInSunWithoutAlerts() {
        givenBalance(900_123_456L);

        monitor.checkBalance();

        verify(balanceRepository).save(argThat(balance ->
                "CATFEE".equals(balance.getId()) && balance.getBalance() == 900_123_456L));
        verifyNoInteractions(alertService, userRepo, telegramState, telegramMessages);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 799_999_999L})
    void opensLowBalanceAlertAndNotifiesEveryAdmin(long balance) {
        givenBalance(balance);
        AppUser firstAdmin = new AppUser();
        firstAdmin.setTelegramId(10L);
        AppUser secondAdmin = new AppUser();
        secondAdmin.setTelegramId(20L);
        UserState firstState = mock(UserState.class);
        UserState secondState = mock(UserState.class);
        when(userRepo.findAllByRole(UserRole.ADMIN)).thenReturn(List.of(firstAdmin, secondAdmin));
        when(telegramState.getOrCreateUserState(10L)).thenReturn(firstState);
        when(telegramState.getOrCreateUserState(20L)).thenReturn(secondState);

        monitor.checkBalance();

        verify(balanceRepository).save(argThat(saved ->
                "CATFEE".equals(saved.getId()) && saved.getBalance() == balance));
        verify(alertService).createCatfeeBalanceLowAlert(balance);
        verify(telegramMessages).sendLowCatfeeBalanceAlert(firstState, balance);
        verify(telegramMessages).sendLowCatfeeBalanceAlert(secondState, balance);
    }

    @Test
    void updatesExistingBalanceWithoutRepeatingAnOpenAlert() {
        givenBalance(123_456_789L);
        ItrxBalance existingBalance = new ItrxBalance();
        existingBalance.setId("CATFEE");
        existingBalance.setBalance(200_000_000L);
        when(balanceRepository.findById("CATFEE")).thenReturn(Optional.of(existingBalance));
        when(alertRepo.findByNameAndStatus(Alert.CATFEE_BALANCE_LOW, AlertStatus.OPEN))
                .thenReturn(new Alert());

        monitor.checkBalance();

        assertEquals(123_456_789L, existingBalance.getBalance());
        verify(balanceRepository).save(existingBalance);
        verifyNoInteractions(alertService, userRepo, telegramState, telegramMessages);
    }

    @ParameterizedTest
    @ValueSource(longs = {THRESHOLD, THRESHOLD + 1})
    void resolvesExistingAlertWhenBalanceRecovers(long balance) {
        givenBalance(balance);
        Alert alert = new Alert();
        alert.setId(42L);
        when(alertRepo.findByNameAndStatus(Alert.CATFEE_BALANCE_LOW, AlertStatus.OPEN)).thenReturn(alert);

        monitor.checkBalance();

        verify(balanceRepository).save(argThat(saved ->
                "CATFEE".equals(saved.getId()) && saved.getBalance() == balance));
        verify(alertService).resolveBalanceLowAlert(42L);
        verifyNoMoreInteractions(alertService);
        verifyNoInteractions(userRepo, telegramState, telegramMessages);
    }

    @Test
    void failedPollLeavesBalanceAndAlertsUntouchedAndAllowsNextPoll() {
        when(restClient.getAccount()).thenThrow(new RuntimeException("CATFEE unavailable"));

        assertDoesNotThrow(monitor::checkBalance);

        verifyNoInteractions(balanceRepository, alertRepo, alertService, userRepo, telegramState, telegramMessages);

        doReturn(accountResponse(THRESHOLD)).when(restClient).getAccount();

        monitor.checkBalance();

        verify(balanceRepository).save(argThat(saved ->
                "CATFEE".equals(saved.getId()) && saved.getBalance() == THRESHOLD));
    }

    private void givenBalance(long balance) {
        when(restClient.getAccount()).thenReturn(accountResponse(balance));
    }

    private CfResponse<CfAccountResponse> accountResponse(long balance) {
        CfAccountResponse account = new CfAccountResponse();
        account.setBalance(balance);
        CfResponse<CfAccountResponse> response = new CfResponse<>();
        response.setCode(0);
        response.setData(account);
        return response;
    }
}
