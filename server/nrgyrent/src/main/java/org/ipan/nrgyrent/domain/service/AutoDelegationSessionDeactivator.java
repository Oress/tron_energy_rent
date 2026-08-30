package org.ipan.nrgyrent.domain.service;

import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;

public interface AutoDelegationSessionDeactivator {
    AutoDelegationSession deactivateSessionManually(Long sessionId);
}
