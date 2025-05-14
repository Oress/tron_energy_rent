package org.ipan.nrgyrent.domain.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.DepositWallet;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.tron.crypto.ECKey;
import org.ipan.nrgyrent.tron.wallet.WalletApi;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepo userRepo;

    @Transactional
    public AppUser createUser(CreateUserCommand command) {
        AppUser appUser = userRepo.findById(command.getTelegramId()).orElse(null);
        if (appUser != null) {
            return appUser;
        }

        appUser = new AppUser();
        appUser.setTelegramId(command.getTelegramId());
        appUser.setCreatedAt(Instant.now());

        EntityManager em = getEntityManager();
        try {
            // TODO: persist cascade?
            DepositWallet depositWallet = generateDepositWallet();
            em.persist(depositWallet);

            Balance depositBalance = new Balance();
            depositBalance.setDepositAddress(depositWallet.getBase58Address());
            em.persist(depositBalance);

            appUser.setBalance(depositBalance);
            em.persist(appUser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return appUser;
    }

    private DepositWallet generateDepositWallet() throws IOException {
        DepositWallet depositWallet = new DepositWallet();

        ECKey privateKeyForNewWallet = WalletApi.generatePrivateKeyForNewWallet();
        byte[] address = privateKeyForNewWallet.getAddress();
        depositWallet.setBase58Address(WalletApi.encode58Check(address));
        // TODO: Encrypt the private key
        depositWallet.setPrivateKeyEncrypted(privateKeyForNewWallet.getPrivateKey());
        return depositWallet;
    }

    public AppUser getById(Long telegramId) {
        return userRepo.findById(telegramId).orElse(null);
    }

    @Lookup
    EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
    
    public List<AppUser> getAllUsers() {
        return userRepo.findAll();
    }
}
