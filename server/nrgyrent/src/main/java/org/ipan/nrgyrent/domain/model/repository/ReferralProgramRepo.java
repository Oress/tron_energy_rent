package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralProgramRepo extends JpaRepository<ReferralProgram, Long> {
    Page<ReferralProgram> findByLabelContainingIgnoreCaseOrderById(String queryStr, PageRequest of);
}
