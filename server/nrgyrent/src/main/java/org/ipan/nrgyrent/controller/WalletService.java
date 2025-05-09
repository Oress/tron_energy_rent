package org.ipan.nrgyrent.controller;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.commands.userwallet.AddOrUpdateUserWalletCommand;
import org.ipan.nrgyrent.commands.userwallet.DeleteUserWalletCommand;
import org.ipan.nrgyrent.model.UserWallet;
import org.ipan.nrgyrent.model.repository.UserWalletRepo;
import org.ipan.nrgyrent.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class WalletService {
    private UserWalletRepo userWalletRepo;

    @Transactional
    public void createWallet(AddOrUpdateUserWalletCommand command) {
        UserWallet userWallet = new UserWallet();
        userWallet.setWalletAddress(command.getWalletAddress());
        userWallet.setCreatedAt(Instant.now());

        getEntityManager().persist(userWallet);
    }

    @PutMapping
    @Transactional(readOnly = true)
    public void updateWallet(AddOrUpdateUserWalletCommand command) {
    }

    @DeleteMapping("/{walletId}")
    @Transactional(readOnly = true)
    public void deleteWallet(DeleteUserWalletCommand command) {
    }

    @PostMapping("/reorder")
    @Transactional(readOnly = true)
    public void reorderWallets(List<Long> walletIds) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AddOrUpdateUserWalletCommand> getWallets() {
        List<UserWallet> userWallets = userWalletRepo.findAll();
        return new ArrayList<>();
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
