package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepo extends JpaRepository<Balance, Long> {
    Page<Balance> findAllByTypeOrderById(BalanceType type, Pageable pageable);
    Page<Balance> findAllByTypeAndLabelContainingIgnoreCaseOrderById(BalanceType group, String label, PageRequest of);
}
