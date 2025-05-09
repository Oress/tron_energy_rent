package org.ipan.nrgyrent.model.repository;

import org.ipan.nrgyrent.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWalletRepo extends JpaRepository<UserWallet, Long> {
    List<UserWallet> findByUserTelegramId(Long userId);
    List<UserWallet> findByUserTelegramIdAndAddress(Long userId, String address);
}
