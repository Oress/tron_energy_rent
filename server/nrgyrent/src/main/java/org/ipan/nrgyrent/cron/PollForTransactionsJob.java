package org.ipan.nrgyrent.cron;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.model.V1AccountsAddressTransactionsGet200Response;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@AllArgsConstructor
public class PollForTransactionsJob {
    private final AccountApi accountApi;
    private final UserService userService;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void scheduleTasks() {
        List<AppUser> allUsers = userService.getAllUsers();

        for (AppUser user : allUsers) {
            V1AccountsAddressTransactionsGet200Response block = accountApi.v1AccountsAddressTransactionsGet(user.getDepositAddress()).block();

            System.out.println("block " + block);
        }
    }
}
