package org.ipan.nrgyrent.cron;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
public class AutoDelegationLowBalanceMonitorJob {
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final BalanceRepo balanceRepo;

    private final Long tgGroupId;

    public AutoDelegationLowBalanceMonitorJob(@Value("${app.notification.tggroupid}")
                                              Long tgGroupId,
                                              AutoDelegationSessionRepo autoDelegationSessionRepo,
                                              TelegramState telegramState,
                                              TelegramMessages telegramMessages,
                                              BalanceRepo balanceRepo) {
        this.autoDelegationSessionRepo = autoDelegationSessionRepo;
        this.telegramState = telegramState;
        this.telegramMessages = telegramMessages;
        this.balanceRepo = balanceRepo;
        this.tgGroupId = tgGroupId;
    }

    public void monitorFowLowBalance() {
        logger.info("Starting low balance monitoring for active auto-delegation sessions");
        List<AutoDelegationSession> allByActive = autoDelegationSessionRepo.findAllByActive(true);
        HashSet<Long> uniqueBalancesIds = new HashSet<>();
        HashSet<Long> uniqueUserIds = new HashSet<>();

        for (AutoDelegationSession session : allByActive) {
            AppUser user = session.getUser();
            Balance balanceToUse = user.getBalanceToUse();
            if (balanceToUse != null) {
                logger.debug("Processing session for user: {}, balance ID: {}, current balance: {}",
                        user.getTelegramId(), balanceToUse.getId(), balanceToUse.getSunBalance());
                if (!uniqueBalancesIds.contains(balanceToUse.getId())) {
                    // if balance has not been processed yet, process it
                    uniqueBalancesIds.add(balanceToUse.getId());
                    if (balanceToUse.getSunBalance() < AppConstants.BALANCE_ALERT_THRESHOLD && !balanceToUse.getIsLow()) {
                        // send notifications here
                        balanceToUse.setIsLow(true);
                        balanceRepo.save(balanceToUse);
                        // send notification to the notification group
                        telegramMessages.sendAutoDelegationAlertBalanceLowAdmin(tgGroupId, balanceToUse.getSunBalance(), user);

                        UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                        telegramMessages.sendAutoDelegationAlertBalanceLowUser(userState, balanceToUse.getSunBalance());

                        logger.warn("Low balance alert triggered - User ID: {}, Current Balance: {} (below threshold: {})",
                                user.getTelegramId(), balanceToUse.getSunBalance(), AppConstants.BALANCE_ALERT_THRESHOLD);
                    } else if (balanceToUse.getSunBalance() >= AppConstants.BALANCE_ALERT_THRESHOLD && balanceToUse.getIsLow()) {
                        // restore alert
                        balanceToUse.setIsLow(false);
                        balanceRepo.save(balanceToUse);
                        logger.info("Balance restored above threshold - User ID: {}, Current Balance: {} (threshold: {})",
                                user.getTelegramId(), balanceToUse.getSunBalance(), AppConstants.BALANCE_ALERT_THRESHOLD);
                    }
                } else {
                    if (!uniqueUserIds.contains(user.getTelegramId())) {
                        // otherwise just send a user with autodelegation session a notificaiton
                        UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
                        telegramMessages.sendAutoDelegationAlertBalanceLowUser(userState, balanceToUse.getSunBalance());
                    }
                }
                uniqueUserIds.add(user.getTelegramId());
            }
        }
        logger.info("Completed low balance monitoring. Processed {} active sessions", allByActive.size());
    }
}
