package org.ipan.nrgyrent.cron;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.List;

import com.google.gson.Gson;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationPaymentStatus;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlStatusResponse;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AmlMonitorCronJobTest {
    @Mock
    private AmlVerificationRepo amlVerificationRepo;
    @Mock
    private AppUserRepo appUserRepo;
    @Mock
    private AmlVerificationService amlVerificationService;
    @Mock
    private NettsRestClient nettsRestClient;
    @Mock
    private TelegramState telegramState;
    @Mock
    private TelegramMessages telegramMessages;

    private AmlMonitorCronJob amlMonitorCronJob;

    @BeforeEach
    void setUp() {
        amlMonitorCronJob = new AmlMonitorCronJob(
                amlVerificationRepo,
                appUserRepo,
                amlVerificationService,
                nettsRestClient,
                telegramState,
                telegramMessages);
    }

    @Test
    void skippedVerificationIsRefundedAndReportedAsFailed() {
        Balance balance = new Balance();
        balance.setId(7L);

        AmlVerification verification = new AmlVerification();
        verification.setId(42L);
        verification.setClientOrderId("AE25C1D6D272136");
        verification.setWalletAddress("TEUbZqhE3LkzsaubZzVLGDvYuYmvgCGXUk");
        verification.setBalance(balance);

        String responseJson = """
                {
                  "success": true,
                  "data": {
                    "client_order_id": "AE25C1D6D272136",
                    "status": "skipped",
                    "address": "TEUbZqhE3LkzsaubZzVLGDvYuYmvgCGXUk",
                    "provider": "elliptic",
                    "report_language": "ru",
                    "reason": "address_inactive",
                    "message": "Address has no activity on blockchain. No charges applied."
                  },
                  "timestamp": "2026-08-30 18:17:54"
                }
                """;
        NettsAmlStatusResponse response = new Gson().fromJson(responseJson, NettsAmlStatusResponse.class);

        AppUser user = new AppUser();
        user.setTelegramId(99L);
        UserState userState = org.mockito.Mockito.mock(UserState.class);

        when(amlVerificationRepo.findAllByPaymentStatus(AmlVerificationPaymentStatus.PENDING))
                .thenReturn(List.of(verification));
        when(nettsRestClient.getAmlStatus("AE25C1D6D272136")).thenReturn(response);
        lenient().when(amlVerificationService.skipVerification(42L, response.getData())).thenReturn(verification);
        lenient().when(appUserRepo.findByBalanceId(7L)).thenReturn(user);
        lenient().when(telegramState.getOrCreateUserState(99L)).thenReturn(userState);

        amlMonitorCronJob.monitorPendingRequests();

        verify(amlVerificationService).skipVerification(42L, response.getData());
        verify(telegramMessages).sendAmlReportFailed(userState, verification);
    }
}
