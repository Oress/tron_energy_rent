package org.ipan.nrgyrent.application.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.events.autotopup.AutoDelegatedManuallyDeactivatedEvent;
import org.ipan.nrgyrent.domain.events.autotopup.AutoEnergyDelegatedEvent;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
@Slf4j
public class AutoDelegationEventApplicationListener {
    private final TelegramState telegramState;
    private final AutoDelegationViews autoDelegationViews;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;

    @EventListener
    @Transactional(readOnly = true)
    public void onEnergyDelegated(AutoEnergyDelegatedEvent event) {
        logger.info("AUTO DELEGATION. Energy has been auto delegated event: {}", event);

        AutoDelegationSession session = autoDelegationSessionRepo.findById(event.getSessionId()).orElse(null);
        if (session == null) {
            logger.error("AUTO DELEGATION. session not found for event: {}", event);
            return;
        }
        UserState userState = telegramState.getOrCreateUserState(session.getUser().getTelegramId());
        autoDelegationViews.updateSessionStatus(userState, session);
    }

    @EventListener
    @Transactional(readOnly = true)
    public void onSessionManuallyDeactivated(AutoDelegatedManuallyDeactivatedEvent event) {
        logger.info("AUTO DELEGATION. Session has been manually deactivated: {}", event);

        AutoDelegationSession session = autoDelegationSessionRepo.findById(event.getSessionId()).orElse(null);
        if (session == null) {
            logger.error("AUTO DELEGATION. session not found for event: {}", event);
            return;
        }
        UserState userState = telegramState.getOrCreateUserState(session.getUser().getTelegramId());
        autoDelegationViews.updateSessionStatus(userState, session);
    }
}
