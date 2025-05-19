package org.ipan.nrgyrent.domain.model.repository;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByCorrelationId(String correlationId);
    Page<Order> findAllByUserTelegramIdOrderByCreatedAtDesc(Long userId, PageRequest pageable);
}
