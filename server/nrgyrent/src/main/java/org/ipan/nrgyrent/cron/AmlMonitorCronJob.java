package org.ipan.nrgyrent.cron;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationStatus;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlStatusResponse;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@AllArgsConstructor
@Slf4j
public class AmlMonitorCronJob {
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_PROCESSING = "processing";

    private final AmlVerificationRepo amlVerificationRepo;
    private final AppUserRepo appUserRepo;
    private final AmlVerificationService amlVerificationService;
    private final NettsRestClient nettsRestClient;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;

    public void monitorPendingRequests() {
        try {
            List<AmlVerification> processing = amlVerificationRepo.findAllByStatus(AmlVerificationStatus.PROCESSING);
            for (AmlVerification v : processing) {
                processVerification(v);
            }
        } catch (Exception e) {
            logger.error("Error monitoring AML requests", e);
        }
    }

    private void processVerification(AmlVerification verification) {
        try {
            String clientOrderId = verification.getClientOrderId();
            if (clientOrderId == null) {
                logger.warn("AML verification {} has no clientOrderId, skipping", verification.getId());
                return;
            }

            NettsAmlStatusResponse statusResponse = nettsRestClient.getAmlStatus(clientOrderId);
            NettsAmlStatusResponse.DataResponse data = statusResponse.getData();
            if (data == null) {
                logger.warn("Empty data in AML status response for verification id: {}", verification.getId());
                return;
            }

            String status = data.getStatus();

            if (STATUS_COMPLETED.equalsIgnoreCase(status)) {
                amlVerificationService.completeVerification(verification.getId(), data);
                notifyUser(verification);
                logger.info("AML verification completed for wallet: {}, id: {}", verification.getWalletAddress(), verification.getId());
            } else if (STATUS_FAILED.equalsIgnoreCase(status)) {
                amlVerificationService.refundVerification(verification.getId());
                notifyUserFailed(verification);
                logger.info("AML verification failed for wallet: {}, id: {}", verification.getWalletAddress(), verification.getId());
            } else if (STATUS_PROCESSING.equalsIgnoreCase(status)) {
                logger.debug("AML verification still processing: id: {}", verification.getId());
            }
        } catch (Exception e) {
            logger.error("Error processing AML verification id: {}, wallet: {}", verification.getId(), verification.getWalletAddress(), e);
        }
    }

    private void notifyUser(AmlVerification verification) {
        try {
            Long userId = resolveUserId(verification);
            if (userId == null) {
                logger.warn("Cannot find user for AML verification id: {}", verification.getId());
                return;
            }
            UserState userState = telegramState.getOrCreateUserState(userId);
            telegramMessages.sendAmlReportCompleted(userState, verification);
        } catch (Exception e) {
            logger.error("Failed to notify user for AML verification id: {}", verification.getId(), e);
        }
    }

    private void notifyUserFailed(AmlVerification verification) {
        try {
            Long userId = resolveUserId(verification);
            if (userId == null) {
                logger.warn("Cannot find user for AML verification id: {}", verification.getId());
                return;
            }
            UserState userState = telegramState.getOrCreateUserState(userId);
            telegramMessages.sendAmlReportFailed(userState, verification);
        } catch (Exception e) {
            logger.error("Failed to notify user for failed AML verification id: {}", verification.getId(), e);
        }
    }

    private Long resolveUserId(AmlVerification verification) {
        if (verification.getBalance() == null) {
            return null;
        }
        Long balanceId = verification.getBalance().getId();
        AppUser user = appUserRepo.findByBalanceId(balanceId);
        if (user != null) {
            return user.getTelegramId();
        }
        var manager = verification.getBalance().getManager();
        if (manager != null) {
            return manager.getTelegramId();
        }
        return null;
    }
}
