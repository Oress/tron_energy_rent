package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.Alert;
import org.ipan.nrgyrent.domain.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepo extends JpaRepository<Alert, Long> {
    Alert findByNameAndStatus(String name, AlertStatus status);
}
