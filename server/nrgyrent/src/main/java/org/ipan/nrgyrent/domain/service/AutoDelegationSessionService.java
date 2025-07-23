package org.ipan.nrgyrent.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.exception.WalletAlreadyHasActiveSessionException;
import org.ipan.nrgyrent.domain.exception.WalletSessionAlreadyDeactivatedException;
import org.ipan.nrgyrent.domain.exception.WalletSessionHasUnexpectedStatusException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEvent;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEventType;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSessionStatus;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AutoDelegationSessionService {
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final AutoDelegationEventRepo autoDelegationEventRepo;
    private final AppUserRepo appUserRepo;

    @Transactional
    public AutoDelegationSession createSession(Long userId, Integer messageId, Long chatId, String walletAddress) {
        List<AutoDelegationSession> activeSessions = autoDelegationSessionRepo.findByAddressAndActive(walletAddress, Boolean.TRUE);

        if (!activeSessions.isEmpty()) {
            throw new WalletAlreadyHasActiveSessionException("Wallet already has active session");
        }

        AppUser user = appUserRepo.findById(userId).orElseThrow(() -> new IllegalStateException("User is not registered"));

        Balance balanceToUse = user.getBalanceToUse();
        Tariff tariffToUse = user.getTariffToUse();
        if (balanceToUse.getSunBalance() < tariffToUse.getMaxAutodelegateFee()) {
            logger.error("Not enough balance to start a session userId {}", user.getTelegramId());
            throw new NotEnoughBalanceException("Not enough balance to start a session");
        }

        AutoDelegationSession newSession = new AutoDelegationSession();
        newSession.setAddress(walletAddress);
        newSession.setUser(user);
        newSession.setMessageToUpdate(messageId);
        newSession.setChatId(chatId);

        autoDelegationSessionRepo.save(newSession);

        return newSession;
    }

    @Transactional
    public AutoDelegationSession deactivate(Long sessionId, AutoDelegationSessionStatus reason) {
        AutoDelegationSession sessionToDeactivate = autoDelegationSessionRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not registered"));

        if (!AutoDelegationSessionStatus.STOPPED_BY_USER.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_INSUFFICIENT_BALANCE.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_NODE_DISCONNECTED.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_SYSTEM_RESTART.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_INACTIVE_WALLET.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_INIT_PROBLEM.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_INACTIVITY.equals(reason)
                && !AutoDelegationSessionStatus.STOPPED_ERROR.equals(reason)
        ) {
            logger.error("Invalid reason for deactivation provided: {}", reason);
            throw new IllegalArgumentException("Invalid reason for deactivation");
        }

        if (!sessionToDeactivate.getActive()) {
            logger.error("Trying to deactivate inactive session id {}", sessionId);
            throw new WalletSessionAlreadyDeactivatedException("Session has already been deactivated");
        }

        if (!AutoDelegationSessionStatus.ACTIVE.equals(sessionToDeactivate.getStatus())) {
            logger.warn("Trying to deactivate session with weird status id {} current status {}", sessionId, sessionToDeactivate.getStatus());
            throw new WalletSessionHasUnexpectedStatusException("Session has unexpected status");
        }

        sessionToDeactivate.setStatus(reason);
        sessionToDeactivate.setActive(Boolean.FALSE);
//        sessionToDeactivate.getEvents().add(generateSessionEndedEvent(sessionToDeactivate));
        return sessionToDeactivate;
    }

    private AutoDelegationEvent generateSessionEndedEvent(AutoDelegationSession session) {
        AutoDelegationEvent event = new AutoDelegationEvent();
        event.setSession(session);
        event.setTimestamp(System.currentTimeMillis());
        event.setType(AutoDelegationEventType.SESSION_STOPPED);
        autoDelegationEventRepo.save(event);
        return event;
    }

    public AutoDelegationEvent createTopupEventForOrder(Order order, Long sessionId, AutoDelegationEventType eventType) {
        AutoDelegationEvent event = new AutoDelegationEvent();
        event.setSession(autoDelegationSessionRepo.findById(sessionId).get());
        event.setOrder(order);
        event.setTimestamp(System.currentTimeMillis());
        event.setType(eventType);
        autoDelegationEventRepo.save(event);
        return event;
    }

    @Transactional
    public void updateLastSmartContractTs(Long sessionId, Long timeStamp) {
        AutoDelegationSession sessionToDeactivate = autoDelegationSessionRepo.findById(sessionId).orElseThrow(() -> new IllegalStateException("Session is not registered"));
        sessionToDeactivate.setLastSmartContractTs(timeStamp);
    }
}
