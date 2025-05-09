package org.ipan.nrgyrent.model.repository;

import org.ipan.nrgyrent.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWalletRepo extends JpaRepository<UserWallet, Long> {
}
