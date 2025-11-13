package org.ipan.nrgyrent.cron;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.ItrxBalanceRepository;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsUserInfoResponse200;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
public class NettsBalanceMonitorCronJob {
    private final ItrxAlertConfig alertConfig;
    private final NettsRestClient nettsRestClient;
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AppUserRepo userRepo;
    private final AlertRepo alertRepo;
    private final AlertService alertService;
    private final ItrxBalanceRepository itrxBalanceRepository;
    private final Long balanceThreshold;

    public NettsBalanceMonitorCronJob(ItrxAlertConfig alertConfig,
                                      NettsRestClient nettsRestClient,
                                      TelegramMessages telegramMessages,
                                      TelegramState telegramState,
                                      AppUserRepo userRepo,
                                      AlertRepo alertRepo,
                                      AlertService alertService,
                                      ItrxBalanceRepository itrxBalanceRepository,
                                      @Value("${app.alerts.itrx-balance.threashold:800000000}") Long balanceThreshold) {
        this.nettsRestClient = nettsRestClient;
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
            NettsUserInfoResponse200 apiStats = nettsRestClient.getStats();
            NettsUserInfoResponse200.Stats stats = apiStats.getStats();

            if (stats == null || stats.getBalance() == null) {
                logger.error("Error when fetching netts io stats");
                return;
            }
            Long currentBalance = BigDecimal.valueOf(stats.getBalance()).multiply(BigDecimal.valueOf(1_000_000D)).longValue();
            Alert activeAlert = alertRepo.findByNameAndStatus(alertConfig.getAlertName(), AlertStatus.OPEN);

            ItrxBalance nettsBalance = itrxBalanceRepository.findById(alertConfig.getBalanceId()).orElse(null);
            if (nettsBalance == null) {
                nettsBalance = new ItrxBalance();
                nettsBalance.setId(alertConfig.getBalanceId());
                nettsBalance.setBalance(currentBalance);
            } else {
                nettsBalance.setBalance(currentBalance);
            }
            itrxBalanceRepository.save(nettsBalance);

            if (currentBalance < balanceThreshold) {
                logger.warn("🚨 ALERT: {} balance is low! Current balance: {}, Threshold: {}", alertConfig.getBalanceId(), currentBalance, balanceThreshold);

                if (activeAlert == null) {
                    alertService.createAlert(currentBalance, alertConfig.getAlertName());

                    List<AppUser> admins = userRepo.findAllByRole(UserRole.ADMIN);
                    for (AppUser admin : admins) {
                        UserState userState = telegramState.getOrCreateUserState(admin.getTelegramId());
                        telegramMessages.sendLowNettsBalanceAlert(userState, currentBalance);
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