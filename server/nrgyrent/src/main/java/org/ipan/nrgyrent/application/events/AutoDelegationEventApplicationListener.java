package org.ipan.nrgyrent.application.events;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.events.autotopup.AutoDelegationSessionCreatedEvent;
import org.ipan.nrgyrent.domain.events.autotopup.AutoDelegatedManuallyDeactivatedEvent;
import org.ipan.nrgyrent.domain.events.autotopup.AutoEnergyDelegatedEvent;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.AutoDelegationViews;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class AutoDelegationEventApplicationListener {
    private final TelegramState telegramState;
    private final AutoDelegationViews autoDelegationViews;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final TelegramMessages telegramMessages;
    private final Long tgGroupId;

    public AutoDelegationEventApplicationListener(
            TelegramState telegramState,
            AutoDelegationViews autoDelegationViews,
            AutoDelegationSessionRepo autoDelegationSessionRepo,
            TelegramMessages telegramMessages,
            @Value("${app.notification.tggroupid}") Long tgGroupId) {
        this.telegramState = telegramState;
        this.autoDelegationViews = autoDelegationViews;
        this.autoDelegationSessionRepo = autoDelegationSessionRepo;
        this.telegramMessages = telegramMessages;
        this.tgGroupId = tgGroupId;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void onSessionCreated(AutoDelegationSessionCreatedEvent event) {
        AutoDelegationSession session = autoDelegationSessionRepo.findById(event.getSessionId()).orElse(null);
        if (session == null) {
            logger.error("AUTO DELEGATION. Session not found for created event: {}", event);
            return;
        }

        try {
            telegramMessages.sendAutoDelegationSessionCreatedAdmin(tgGroupId, session);
        } catch (Exception e) {
            logger.error("Could not send auto delegation session created notification. Session id {}",
                    session.getId(), e);
        }
    }

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
