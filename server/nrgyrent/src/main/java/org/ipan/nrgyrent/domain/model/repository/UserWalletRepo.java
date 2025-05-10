package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserWalletRepo extends JpaRepository<UserWallet, Long> {
    List<UserWallet> findByUserTelegramId(Long userId);
    List<UserWallet> findByUserTelegramIdAndAddress(Long userId, String address);
}
