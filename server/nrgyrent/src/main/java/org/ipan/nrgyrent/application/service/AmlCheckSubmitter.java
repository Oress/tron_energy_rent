package org.ipan.nrgyrent.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlCreateResponse200;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Charges and submits AML verifications for the provider(s) configured by the user.
 * <p>
 * When the user selected {@link AmlProvider#ELLIPTIC_AND_BITOK} a separate verification is created and
 * submitted for each underlying provider, so the AML monitor reports each one independently (two reports).
 * For the combined case each report is delivered as its own message (messageToUpdate is not reused) so the
 * second report does not overwrite the first.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AmlCheckSubmitter {
    private final AmlVerificationService amlVerificationService;
    private final NettsRestClient nettsRestClient;

    /**
     * @return the number of verifications that were successfully submitted to the provider.
     * @throws NotEnoughBalanceException if the balance did not cover a single verification (nothing submitted).
     */
    public int submitChecks(AppUser user, String walletAddress, Long chatId, Integer messageToUpdate) {
        List<AmlProvider> providers = user.getAmlProvider().concreteProviders();
        boolean combined = providers.size() > 1;

        int submitted = 0;
        NotEnoughBalanceException insufficient = null;
        for (AmlProvider provider : providers) {
            AmlVerification verification = null;
            try {
                // Combined reports are sent as fresh messages so they don't overwrite each other.
                Integer msgToUpdate = combined ? null : messageToUpdate;
                verification = amlVerificationService.createPendingVerification(
                        user.getTelegramId(), walletAddress, provider, chatId, msgToUpdate);
                NettsAmlCreateResponse200 response = nettsRestClient.createAmlRequest(
                        walletAddress, provider, user.getLanguageCode());
                amlVerificationService.markProcessing(verification.getId(), response.getData());
                submitted++;
                logger.info("AML request submitted for wallet {} provider {} verification id: {}",
                        walletAddress, provider, verification.getId());
            } catch (NotEnoughBalanceException e) {
                insufficient = e;
                logger.warn("Not enough balance for AML provider {} wallet {}: {}", provider, walletAddress, e.getMessage());
                if (submitted > 0) {
                    // At least one report is on its way; don't fail the whole check over the remaining provider.
                    break;
                }
            } catch (Exception e) {
                logger.error("Failed to submit AML request for wallet {} provider {}: {}",
                        walletAddress, provider, e.getMessage());
                if (verification != null) {
                    amlVerificationService.refundVerification(verification.getId());
                }
            }
        }

        if (submitted == 0 && insufficient != null) {
            throw insufficient;
        }
        return submitted;
    }
}
