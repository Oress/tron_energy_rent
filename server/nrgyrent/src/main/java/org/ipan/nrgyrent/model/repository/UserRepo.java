package org.ipan.nrgyrent.model.repository;

import org.ipan.nrgyrent.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<AppUser, Long> {
}
