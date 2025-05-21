package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.WithdrawalOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawalOrderRepo extends JpaRepository<WithdrawalOrder, Long> {
}
