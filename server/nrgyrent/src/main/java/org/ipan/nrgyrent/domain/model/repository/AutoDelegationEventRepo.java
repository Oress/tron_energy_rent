package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoDelegationEventRepo extends JpaRepository<AutoDelegationEvent, Long> {
}
