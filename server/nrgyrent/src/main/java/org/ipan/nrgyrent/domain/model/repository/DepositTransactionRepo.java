package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositTransactionRepo extends JpaRepository<DepositTransaction, Long> {
    DepositTransaction findBySystemOrder(Order systemOrder);

    DepositTransaction findBySystemOrderCorrelationId(String systemOrderCorrelationId);
}
