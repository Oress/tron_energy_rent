package org.ipan.nrgyrent.domain.model.repository;

import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManagedWalletRepo extends JpaRepository<ManagedWallet, String> {
}
