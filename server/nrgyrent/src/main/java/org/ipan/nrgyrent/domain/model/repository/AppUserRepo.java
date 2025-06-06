package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser, Long> {
    Set<AppUser> findAllByGroupBalanceId(Long balanceId);
    AppUser findByBalanceId(Long balanceId);

    @Query(value = """
            SELECT u FROM AppUser u
                WHERE LOWER(u.telegramUsername) LIKE LOWER(:username) OR LOWER(u.telegramFirstName) LIKE LOWER(:firstName)
                ORDER BY u.telegramUsername, u.telegramFirstName, u.id
            """
            )
    Page<AppUser> searchByUsernameAndFirstname(String username, String firstName, PageRequest of);

    @Query("select u from AppUser u join u.referralProgram rp where rp.id = :balanceRefProgramId")
    List<AppUser> findAllByBalRefProgId(Long balanceRefProgramId);
}
