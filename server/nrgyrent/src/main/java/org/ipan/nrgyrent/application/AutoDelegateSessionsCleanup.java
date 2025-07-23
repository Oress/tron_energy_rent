package org.ipan.nrgyrent.application;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.events.FullNodeDisconnectedEvent;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class AutoDelegateSessionsCleanup {
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final EnergyService energyService;

    @Transactional
//    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        logger.info("Application shutting down - deactivating all active delegation sessions");
        List<AutoDelegationSession> activeSessions = autoDelegationSessionRepo.findByActive(true);

        for (AutoDelegationSession session : activeSessions) {
            try {
                energyService.deactivateSessionSystemRestart(session.getId());
            } catch (Exception e) {
                logger.error("Error while deactivating session {}: {}", session.getId(), e.getMessage());
            }
        }
        logger.info("Finished deactivating {} delegation sessions", activeSessions.size());
    }

    @Transactional
    @Async
//    @EventListener(FullNodeDisconnectedEvent.class)
    public void onNodeDisconnected() {
        logger.info("Application shutting down - deactivating all active delegation sessions");
        List<AutoDelegationSession> activeSessions = autoDelegationSessionRepo.findByActive(true);

        for (AutoDelegationSession session : activeSessions) {
            try {
                energyService.deactivateSessionNodeDisconnected(session.getId());
            } catch (Exception e) {
                logger.error("Error while deactivating session {}: {}", session.getId(), e.getMessage());
            }
        }
        logger.info("Finished deactivating {} delegation sessions", activeSessions.size());
    }
}
