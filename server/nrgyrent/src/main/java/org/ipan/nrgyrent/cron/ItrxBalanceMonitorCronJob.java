package org.ipan.nrgyrent.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.Alert;
import org.ipan.nrgyrent.domain.model.AlertStatus;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItrxBalanceMonitorCronJob {
    private final RestClient restClient;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AppUserRepo userRepo;
    private final AlertRepo alertRepo;
    private final AlertService alertService;

    @Value("${app.alerts.itrx-balance.threashold:800000000}")
    private Long balanceThreshold;

    @Scheduled(fixedDelayString = "${app.alerts.itrx-balance.interval:180}", timeUnit = TimeUnit.SECONDS)
    public void checkBalance() {
        try {
            ApiUsageResponse apiStats = restClient.getApiStats();
            Long currentBalance = apiStats.getBalance();
            Alert activeAlert = alertRepo.findByNameAndStatus(Alert.ITRX_BALANCE_LOW, AlertStatus.OPEN);

            if (currentBalance < balanceThreshold) {
                logger.warn("🚨 ALERT: ITRX balance is low! Current balance: {}, Threshold: {}", currentBalance, balanceThreshold);

                if (activeAlert == null) {
                    alertService.createItrxBalanceLowAlert(currentBalance);

                    List<AppUser> admins = userRepo.findAllByRole(UserRole.ADMIN);
                    for (AppUser admin : admins) {
                        UserState userState = telegramState.getOrCreateUserState(admin.getTelegramId());
                        telegramMessages.sendLowItrxBalanceAlert(userState, currentBalance);
                    }
                }
            } else if (activeAlert != null) {
                alertService.resolveItrxBalanceLowAlert(activeAlert.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to check ITRX balance", e);
        }
    }
}