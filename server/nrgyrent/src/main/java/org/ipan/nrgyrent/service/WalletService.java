package org.ipan.nrgyrent.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.model.AppUser;
import org.ipan.nrgyrent.model.UserWallet;
import org.ipan.nrgyrent.model.repository.UserWalletRepo;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class WalletService {
    private UserWalletRepo userWalletRepo;

    @Transactional
    public UserWallet createWallet(AddOrUpdateUserWalletCommand command) {
        EntityManager em = getEntityManager();

        List<UserWallet> alreadyPresent = userWalletRepo.findByUserTelegramIdAndAddress(command.getUserId(), command.getWalletAddress());
        if (!alreadyPresent.isEmpty()) {
            throw new IllegalArgumentException("Wallet already exists");
        }

        AppUser user = em.getReference(AppUser.class, command.getUserId());

        UserWallet userWallet = new UserWallet();
        userWallet.setAddress(command.getWalletAddress());
        userWallet.setCreatedAt(Instant.now());
        userWallet.setUser(user);

        em.persist(userWallet);

        return userWallet;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserWallet> getWallets(Long userId) {
        return userWalletRepo.findByUserTelegramId(userId);
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
