package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
        appUser.setActive(true);
        appUser.setCreatedAt(Instant.now());

        getEntityManager().persist(appUser);

        return appUser;
    }

    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
