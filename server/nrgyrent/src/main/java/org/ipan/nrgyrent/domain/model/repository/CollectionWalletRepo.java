package org.ipan.nrgyrent.domain.model.repository;

import java.util.List;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionWalletRepo extends JpaRepository<CollectionWallet, Long> {
    List<CollectionWallet> findAllByIsActive(Boolean isActive);
    CollectionWallet findFirstByIsActive(Boolean isActive);
}
