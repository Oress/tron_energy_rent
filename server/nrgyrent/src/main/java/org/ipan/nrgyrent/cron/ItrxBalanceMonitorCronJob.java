package org.ipan.nrgyrent.cron;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.ItrxBalanceRepository;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

//@Service
@Slf4j
public class ItrxBalanceMonitorCronJob {
    private final ItrxAlertConfig alertConfig;
    private final RestClient restClient;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AppUserRepo userRepo;
    private final AlertRepo alertRepo;
    private final AlertService alertService;
    private final ItrxBalanceRepository itrxBalanceRepository;
    private final Long balanceThreshold;

    public ItrxBalanceMonitorCronJob(ItrxAlertConfig alertConfig,
                                     RestClient restClient,
                                     TelegramMessages telegramMessages,
                                     TelegramState telegramState,
                                     AppUserRepo userRepo,
                                     AlertRepo alertRepo,
                                     AlertService alertService,
                                     ItrxBalanceRepository itrxBalanceRepository,
                                     @Value("${app.alerts.itrx-balance.threashold:800000000}") Long balanceThreshold) {
        this.restClient = restClient;
        this.telegramMessages = telegramMessages;
        this.telegramState = telegramState;
        this.userRepo = userRepo;
        this.alertRepo = alertRepo;
        this.alertConfig = alertConfig;
        this.alertService = alertService;
        this.itrxBalanceRepository = itrxBalanceRepository;
        this.balanceThreshold = balanceThreshold;
    }

    public void checkBalance() {
        try {
            ApiUsageResponse apiStats = restClient.getApiStats();
            Long currentBalance = apiStats.getBalance();
            Alert activeAlert = alertRepo.findByNameAndStatus(alertConfig.getAlertName(), AlertStatus.OPEN);

            ItrxBalance itrxBalance = itrxBalanceRepository.findById(alertConfig.getBalanceId()).orElse(null);
            if (itrxBalance == null) {
                itrxBalance = new ItrxBalance();
                itrxBalance.setId(alertConfig.getBalanceId());
                itrxBalance.setBalance(currentBalance);
            } else {
                itrxBalance.setBalance(currentBalance);
            }
            itrxBalanceRepository.save(itrxBalance);

            if (currentBalance < balanceThreshold) {
                logger.warn("🚨 ALERT: {} balance is low! Current balance: {}, Threshold: {}", alertConfig.getBalanceId(), currentBalance, balanceThreshold);

                if (activeAlert == null) {
                    alertService.createAlert(currentBalance, alertConfig.getAlertName());

                    List<AppUser> admins = userRepo.findAllByRole(UserRole.ADMIN);
                    for (AppUser admin : admins) {
                        UserState userState = telegramState.getOrCreateUserState(admin.getTelegramId());
                        if (alertConfig.isItrx()) {
                            telegramMessages.sendLowItrxBalanceAlert(userState, currentBalance);
                        } else {
                            telegramMessages.sendLowTrxxBalanceAlert(userState, currentBalance);
                        }
                    }
                }
            } else if (activeAlert != null) {
                alertService.resolveBalanceLowAlert(activeAlert.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to check {} balance", alertConfig.getBalanceId(), e);
        }
    }
}