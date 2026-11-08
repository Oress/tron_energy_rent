package org.ipan.nrgyrent.fixedfloat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.DepositTransactionRepo;
import org.ipan.nrgyrent.domain.model.repository.FixedFloatReviewNotificationRepo;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixedFloatReviewNotificationService {
    private final DepositTransactionRepo deposits;
    private final FixedFloatReviewNotificationRepo notifications;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;

    @Transactional(rollbackFor = Exception.class)
    public void notifyOnce(Long depositId, Long telegramId) {
        // Serialize delivery with other pollers and settlement; never notify a completed deposit.
        DepositTransaction deposit = deposits.findLockedById(depositId);
        if (deposit == null || deposit.getExchangeProvider() != UsdtExchangeProvider.FIXEDFLOAT
                || deposit.getStatus() != DepositStatus.USDT_FF_REVIEW_REQUIRED) return;
        var id = new FixedFloatReviewNotification.Id(depositId, telegramId);
        if (notifications.existsById(id)) {
            logger.debug("FixedFloat review notice already delivered: depositId={}, orderId={}, telegramId={}",
                    depositId, deposit.getExchangeOrderId(), telegramId);
            return;
        }

        logger.debug("FixedFloat sending review notice: depositId={}, orderId={}, telegramId={}",
                depositId, deposit.getExchangeOrderId(), telegramId);
        telegramMessages.sendFixedFloatReviewNotification(telegramState.getOrCreateUserState(telegramId), deposit);
        logger.info("FixedFloat review notice accepted by Telegram: depositId={}, orderId={}, telegramId={}",
                depositId, deposit.getExchangeOrderId(), telegramId);
        // Failed sends remain retryable. Keep a separate receipt for each group member.
        notifications.saveAndFlush(new FixedFloatReviewNotification(id, Instant.now()));
    }
}
