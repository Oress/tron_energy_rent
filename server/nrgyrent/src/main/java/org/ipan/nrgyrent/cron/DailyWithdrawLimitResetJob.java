package org.ipan.nrgyrent.cron;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DailyWithdrawLimitResetJob {
    private final BalanceRepo balanceRepo;

    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void resetDailyLimits() {
        List<Balance> balancesToReset = balanceRepo.findAllByIsActive(true);
        
        for (Balance balance : balancesToReset) {
            balance.resetDailyWithdrawalLimit();
        }
    }
}