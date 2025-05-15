package org.ipan.nrgyrent.cron;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.cron.tron.transaction.enabled")
public class PollForTransactionsJob {
    private static final Integer BATCH_SIZE = 10;

    private final UserService userService;
    private final PollForTransactionsJobHelper helper;

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void scheduleTasks() {
        List<Balance> userWallets = userService.getAllUsers()
                .stream()
                .map(AppUser::getBalance)
                .toList();

        for (int i = 0; i < userWallets.size(); i += BATCH_SIZE) {
            List<Balance> batch = userWallets.subList(i, Math.min(i + BATCH_SIZE, userWallets.size()));
            helper.processBatch(batch);
        }
    }
}
