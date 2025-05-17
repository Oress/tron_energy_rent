package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByCorrelationId(String correlationId);
}
