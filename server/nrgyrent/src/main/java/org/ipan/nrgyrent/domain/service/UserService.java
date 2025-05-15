package org.ipan.nrgyrent.domain.service;

import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final BalanceService balanceService;

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
        Balance individualDepositBalance = balanceService.createIndividualBalance(appUser);

        appUser.setBalance(individualDepositBalance);
        em.persist(appUser);

        return appUser;
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

    public List<AppUser> getByIds(List<Long> userIds) {
        return userRepo.findAllById(userIds);
    }
}
