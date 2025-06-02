package org.ipan.nrgyrent.domain.model.repository;

import java.util.Set;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Page<AppUser> findAllByTelegramUsernameContainingIgnoreCaseOrTelegramFirstNameContainingIgnoreCaseOrderByTelegramId(String username, String firstName, PageRequest of);
    Set<AppUser> findAllByGroupBalanceId(Long balanceId);
    AppUser findByBalanceId(Long balanceId);
}
