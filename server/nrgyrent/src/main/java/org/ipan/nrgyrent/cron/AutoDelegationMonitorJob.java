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
    public static final int MAX_IDLE_MINUTES = 24 * 60 - 20;

    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final OrderRepo orderRepo;
    private final EnergyService energyService;
    private final AutoDelegationViews autoDelegationViews;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;

    // TODO: can be optimized
    public void monitorForInactiveSessions() {
        List<AutoDelegationSession> allByActive = autoDelegationSessionRepo.findAllByActive(true);

        for (AutoDelegationSession session : allByActive) {
            Order top1ByAutoDelegationSessionIdOrderByCreatedAtDesc = orderRepo.findTop1ByAutoDelegationSessionIdOrderByCreatedAtDesc(session.getId());

            Instant dtFrom = top1ByAutoDelegationSessionIdOrderByCreatedAtDesc != null
                    ? top1ByAutoDelegationSessionIdOrderByCreatedAtDesc.getCreatedAt()
                    : session.getCreatedAt();

            Duration duration = Duration.between(Instant.now(), dtFrom);
            if (duration.abs().toMinutes() >= MAX_IDLE_MINUTES) {
                AppUser user = session.getUser();
                Long telegramId = user.getTelegramId();
                energyService.deactivateSessionLowBalance(session.getId());
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
            }
        }
    }
}
