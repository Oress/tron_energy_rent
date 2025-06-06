package org.ipan.nrgyrent.cron;

import java.util.List;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.ReferralCommission;
import org.ipan.nrgyrent.domain.model.ReferralCommissionStatus;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralCommissionRepo;
import org.springframework.context.annotation.Configuration;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@AllArgsConstructor
@Slf4j
public class ReferralCommissionHelper {
    private final BalanceRepo balanceRepo;
    private final ReferralCommissionRepo referralCommissionRepo;

    @Transactional
    // @Async
    public void processBalanceWithPendingCommissions(Long balanceId) {
        Balance balance = balanceRepo.findById(balanceId).orElse(null);

        if (balance == null) {
            logger.error("Referral commission batch belong to non existing balance {}", balanceId);
            return;
        }

        List<ReferralCommission> batch = referralCommissionRepo.findAllPendingByBalanceId(balanceId);
        if (batch == null || batch.isEmpty()) {
            logger.warn("Referral commission batch is empty");
            return;
        }

        Long totalSum = 0L;
        for (ReferralCommission commission : batch) {
            if (ReferralCommissionStatus.COLLECTED.equals(commission.getStatus())) {
                logger.error("Referral commission batch includes COLLECTED item {}", commission.getId());
                continue;
            }

            Long sunAmount = commission.getAmountSun();
            if (sunAmount <= 0) {
                logger.error("Referral commission amount is <= 0 commission id: {}", commission.getId());
                continue;
            }
            totalSum += sunAmount;
        }

        logger.info("Total Referral commission for balance: {} is {}", balance.getId(), totalSum);
        balance.makeDeposit(totalSum);
    }
}
