package org.ipan.nrgyrent.domain.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.ManagedWallet;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;

@Service
public class CollectionWalletService {
    private final ManagedWalletService managedWalletService;
    private final CollectionWalletRepo collectionWalletRepo;
    private final Integer ensureWalletsCount;
    
    public CollectionWalletService(
        ManagedWalletService managedWalletService,
        CollectionWalletRepo collectionWalletRepo,
        @Value("${app.wallet.sweeping.ensure-count}") Integer ensureWalletsCount
    ) {
        this.managedWalletService = managedWalletService;
        this.collectionWalletRepo = collectionWalletRepo;
        this.ensureWalletsCount = ensureWalletsCount;
    }

    @Transactional
    @SneakyThrows
    public void ensureWalletsAreCreated()  {
        List<CollectionWallet> wallets = collectionWalletRepo.findAllByIsActive(true);
        if (wallets.size() < ensureWalletsCount) {
            EntityManager em = getEntityManager();
            List<CollectionWallet> newWallets = new ArrayList<>();

            for (int i = wallets.size(); i < ensureWalletsCount; i++) {
                ManagedWallet managedWallet = managedWalletService.generateManagedWallet();
                em.persist(managedWallet);
                CollectionWallet wallet = new CollectionWallet();
                wallet.setWalletAddress(managedWallet.getBase58Address());
                newWallets.add(wallet);
            }
            collectionWalletRepo.saveAll(newWallets);
        }
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
