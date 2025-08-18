package org.ipan.nrgyrent.domain.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.ItrxBalanceRepository;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceEventDomainListener {
    private static final String CATFEE = "CATFEE";

    private final ItrxBalanceRepository itrxBalanceRepository;
    private final AlertRepo alertRepo;
    private final AlertService alertService;
    private final AppUserRepo appUserRepo;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;

    @Value("${app.alerts.itrx-balance.threashold:800000000}")
    private Long balanceThreshold;

    @Async
    @EventListener
    public void onBalanceUpdated(BalanceUpdatedEvent event) {
        logger.trace("Balance updated event received: {}", event);
        try {
            Long currentBalance = event.getNewBalance();
            Alert activeAlert = alertRepo.findByNameAndStatus(Alert.CATFEE_BALANCE_LOW, AlertStatus.OPEN);

            ItrxBalance itrxBalance = itrxBalanceRepository.findById(CATFEE).orElse(null);
            if (itrxBalance == null) {
                itrxBalance = new ItrxBalance();
                itrxBalance.setId(CATFEE);
                itrxBalance.setBalance(currentBalance);
            } else {
                itrxBalance.setBalance(currentBalance);
            }
            itrxBalanceRepository.save(itrxBalance);

            if (currentBalance < balanceThreshold) {
                logger.warn("🚨 ALERT: CATFEE balance is low! Current balance: {}, Threshold: {}", currentBalance, balanceThreshold);

                if (activeAlert == null) {
                    alertService.createCatfeeBalanceLowAlert(currentBalance);

                    List<AppUser> admins = appUserRepo.findAllByRole(UserRole.ADMIN);
                    for (AppUser admin : admins) {
                        UserState userState = telegramState.getOrCreateUserState(admin.getTelegramId());
                        telegramMessages.sendLowCatfeeBalanceAlert(userState, currentBalance);
                    }
                }
            } else if (activeAlert != null) {
                alertService.resolveBalanceLowAlert(activeAlert.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to check ITRX balance", e);
        }

    }
}
