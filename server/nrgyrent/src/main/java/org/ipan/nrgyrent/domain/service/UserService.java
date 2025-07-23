package org.ipan.nrgyrent.domain.service;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.LiquibaseParameters;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.repository.BalanceReferralProgramRepo;
import org.ipan.nrgyrent.domain.model.repository.UserRepo;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.domain.service.commands.users.CreateUserCommand;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {
    private final UserRepo userRepo;
    private final BalanceService balanceService;
    private final BalanceReferralProgramRepo balanceReferralProgramRepo;
    private final ReferalProgramService referalProgramService;
    private final LiquibaseParameters liquibaseParameters;

    @Transactional
    public AppUser setShowWalletOption(Long userId, Boolean value) {
        AppUser appUser = userRepo.findById(userId).orElse(null);
        appUser.setShowWalletsMenu(value);
        return appUser;
    }

    @Transactional
    public AppUser createUser(CreateUserCommand command) {
        AppUser appUser = userRepo.findById(command.getTelegramId()).orElse(null);
        if (appUser != null) {
            return appUser;
        }

        appUser = new AppUser();
        updateModelFromCommand(command, appUser);

        Balance individualDepositBalance = balanceService.createIndividualBalance(appUser, command);

        appUser.setBalance(individualDepositBalance);
        userRepo.save(appUser);

        // create default ref. program
        referalProgramService.createReferalProgramForUser(appUser.getTelegramId(), liquibaseParameters.getDefaultRefProgramId());

        return appUser;
    }

    @Transactional
    public AppUser updateUser(CreateUserCommand command) {
        AppUser appUser = userRepo.findById(command.getTelegramId()).orElse(null);

        updateModelFromCommand(command, appUser);

        return appUser;
    }

    @Transactional
    public AppUser setLanguage(Long userId, String language) {
        AppUser appUser = userRepo.findById(userId).orElse(null);

        if (!"en".equals(language) && !"ru".equals(language) && !"uk".equals(language)) {
            throw new IllegalArgumentException("language is neither en nor ru nor uk");
        }

        appUser.setLanguageCode(language);

        return appUser;
    }

    @Transactional
    public void deactivateUser(Long selectedUserId) {
        AppUser appUser = userRepo.findById(selectedUserId).orElse(null);
        if (appUser != null) {
            if (appUser.isInGroup()) {
                balanceService.removeUsersFromTheGroupBalance(appUser.getGroupBalance().getId(), List.of(new TgUserId(selectedUserId, null, null)));
            }
            appUser.setDisabled(true);
            balanceService.deactivateUserBalance(appUser.getBalance().getId());
            referalProgramService.removeRefProgram(appUser.getTelegramId());
        }
    }

    private void updateModelFromCommand(CreateUserCommand command, AppUser appUser) {
        appUser.setTelegramId(command.getTelegramId());
        appUser.setTelegramUsername(command.getUsername());
        appUser.setTelegramFirstName(command.getFirstName());
        if (command.getLanguageCode() != null) {
            appUser.setLanguageCode(command.getLanguageCode());
        }

        String link = command.getRefferalLink();
        if (link != null && !link.isEmpty()) {
            BalanceReferralProgram refferalProgram = balanceReferralProgramRepo.findByLink(link);

            if (refferalProgram != null) {
                appUser.setReferralProgram(refferalProgram);
            } else {
                logger.error("Cannot find refferal program by link {} user id {} login {}", link, command.getTelegramId(), command.getUsername());
            }
        }
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
