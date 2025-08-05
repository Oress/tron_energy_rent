package org.ipan.nrgyrent.domain.service;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.repository.UserWalletRepo;
import org.ipan.nrgyrent.domain.service.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.domain.service.commands.userwallet.DeleteUserWalletCommand;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserWalletService {
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
        userWallet.setLabel(command.getLabel());
        userWallet.setUser(user);

        em.persist(userWallet);

        return userWallet;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserWallet> getWallets(Long userId) {
        return userWalletRepo.findByUserTelegramIdOrderByCreatedAt(userId);
    }

    @Transactional
    public void deleteWallet(DeleteUserWalletCommand command) {
        EntityManager em = getEntityManager();

        UserWallet userWallet = em.find(UserWallet.class, command.getWalletId());
        if (userWallet == null) {
            throw new IllegalArgumentException("Wallet not found");
        }

        em.remove(userWallet);
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
