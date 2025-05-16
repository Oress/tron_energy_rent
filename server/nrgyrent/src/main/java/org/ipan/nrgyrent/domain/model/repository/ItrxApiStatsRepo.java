package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.ItrxApiStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItrxApiStatsRepo extends JpaRepository<ItrxApiStats, Long> {
}
