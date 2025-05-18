package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.ManualBalanceAdjustmentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManualBalanceAdjustmentActionRepo extends JpaRepository<ManualBalanceAdjustmentAction, Long> {
}
