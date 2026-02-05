package org.ipan.nrgyrent.cron;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.ReferralCommission;
import org.ipan.nrgyrent.domain.model.ReferralCommissionDeposit;
import org.ipan.nrgyrent.domain.model.ReferralCommissionStatus;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralCommissionDepositRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralCommissionRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
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
    private final TelegramMessages telegramMessages;
    private final TelegramState telegramState;
    private final AppUserRepo userRepo;
    private final ReferralCommissionDepositRepo referralCommissionDepositRepo;


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

        ReferralCommissionDeposit deposit = new ReferralCommissionDeposit();
        deposit.setBalance(balance);
        deposit.setAmountSun(0L);
        referralCommissionDepositRepo.save(deposit);

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
            commission.setStatus(ReferralCommissionStatus.COLLECTED);
            commission.setDeposit(deposit);
        }

        logger.info("Total Referral commission for balance: {} is {}", balance.getId(), totalSum);
        balance.makeDeposit(totalSum);
        deposit.setAmountSun(totalSum);

        AppUser user = userRepo.findByBalanceId(balanceId);
        if (user != null) {
            UserState userState = telegramState.getOrCreateUserState(user.getTelegramId());
            // in case someone blocked the bot.
            try {
                telegramMessages.sendReferalPaymentNotification(userState, totalSum);
            } catch (Exception e) {
                logger.error("Could not send notification to the balance: {}, user id: {}", balanceId, userState.getTelegramId());
            }
        } else {
            logger.error("Referral commission for balance {} is NOT FOR USER", balanceId);
        }
        
    }
}
