package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
public class AutoDelegationMonitorJob {
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final EnergyService energyService;
    private final AutoDelegationViews autoDelegationViews;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;

    public void monitorForInactiveSessions() {
        try {
            List<AutoDelegationSession> allByActive = autoDelegationSessionRepo.findAllByActive(true);

            for (AutoDelegationSession session : allByActive) {
                // param responsible for auto deactivation is unused_times_threshold
                // https://develop.itrx.io/api/order-count-policy.html
                boolean isPaused = energyService.isSessionPausedOnService(session);
                if (isPaused) {
                    logger.info("Sesssion was disabled by service. id: {}, service: {}", session.getId(), session.getAddress());
                    AppUser user = session.getUser();
                    Long telegramId = user.getTelegramId();

                    energyService.deactivateSessionInactivity(session.getId());
                    UserState userState = telegramState.getOrCreateUserState(telegramId);
                    autoDelegationViews.autoDelegateSessionStoppedInactivity(userState, session);
                    List<UserWallet> userWallets = Collections.emptyList();
                    if (user.getShowWalletsMenu()) {
                        userWallets = userWalletService.getWallets(user.getTelegramId());
                    }
                    Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
                    telegramState.updateUserState(userState.getTelegramId(), userState
                            .withState(States.MAIN_MENU)
                            .withMenuMessageId(newMenuMsg.getMessageId()));
                    logger.info("PROCESSED. Sesssion was disabled by service id: {}, service: {}", session.getId(), session.getAddress());
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    logger.error("Could not sleep ");
                }
            }
        } catch (Exception e) {
            logger.error("Something went wrong when monitoring for inactive auto delegation sessions", e);
        }
    }
}
