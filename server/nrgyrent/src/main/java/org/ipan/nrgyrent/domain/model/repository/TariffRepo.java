package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffRepo extends JpaRepository<Tariff, Long> {
    @Query("select t from Tariff t where t.id = 1")
    Tariff getDefaultTariff();

    Page<Tariff> findByLabelContainingIgnoreCaseOrderById(String label, PageRequest page);
    Page<Tariff> findByActiveAndLabelContainingIgnoreCaseOrderById(Boolean active, String label, PageRequest page);
}
