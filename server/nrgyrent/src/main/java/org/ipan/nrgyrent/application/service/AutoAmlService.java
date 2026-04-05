package org.ipan.nrgyrent.application.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSession;
import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSessionDeactivationReason;
import org.ipan.nrgyrent.domain.model.repository.AutoAmlSessionRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlCreateResponse200;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.views.AmlViews;
import org.ipan.nrgyrent.telegram.views.AutoAmlViews;
import org.ipan.nrgyrent.tron.node.events.AutoAmlWatchlist;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class AutoAmlService {
    private final AutoAmlSessionRepo autoAmlSessionRepo;
    private final AutoAmlWatchlist autoAmlWatchlist;
    private final AmlVerificationService amlVerificationService;
    private final NettsRestClient nettsRestClient;
    private final UserService userService;
    private final AutoAmlViews autoAmlViews;
    private final AmlViews amlViews;
    private final TelegramState telegramState;
    private final UserWalletService userWalletService;
    private final TelegramMessages telegramMessages;

    public AutoAmlSession createSession(Long userId, String address, Long thresholdSun, Long chatId, Integer messageToUpdate) {
        var existing = autoAmlSessionRepo.findByAddressAndUser_TelegramIdAndActive(address, userId, true);
        if (!existing.isEmpty()) {
            throw new IllegalStateException("User " + userId + " already has an active auto-AML session for address: " + address);
        }

        AppUser user = userService.getById(userId);

        AutoAmlSession session = new AutoAmlSession();
        session.setAddress(address);
        session.setUser(user);
        session.setThresholdSun(thresholdSun);
        session.setChatId(chatId);
        session.setMessageToUpdate(messageToUpdate);
        session.setActive(true);

        session = autoAmlSessionRepo.save(session);
        autoAmlWatchlist.addSession(address, session.getId(), userId, thresholdSun);

        logger.info("Created auto-AML session id: {} for user: {} address: {} threshold: {} USDT",
                session.getId(), userId, address, FormattingTools.formatUsdt(thresholdSun));
        return session;
    }

    public AutoAmlSession deactivateSession(Long sessionId, AutoAmlSessionDeactivationReason reason) {
        AutoAmlSession session = autoAmlSessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Auto-AML session not found: " + sessionId));

        session.setActive(false);
        session.setDeactivatedAt(Instant.now());
        session.setDeactivationReason(reason);
        autoAmlSessionRepo.save(session);
        autoAmlWatchlist.removeSession(session.getAddress(), session.getUser().getTelegramId());

        logger.info("Deactivated auto-AML session id: {} address: {} reason: {}", sessionId, session.getAddress(), reason);
        return session;
    }

    @Async
    public void triggerAutoAmlCheck(Long sessionId, String senderAddress, Long amountSun) {
        AutoAmlSession session = autoAmlSessionRepo.findById(sessionId).orElse(null);
        if (session == null || !Boolean.TRUE.equals(session.getActive())) {
            logger.warn("Auto-AML session {} not found or inactive, skipping check", sessionId);
            return;
        }

        Long userId = session.getUser().getTelegramId();
        AppUser user = userService.getById(userId);
        AmlProvider provider = user.getAmlProvider();

        logger.info("Auto-AML triggered for session {} sender {} amount {} USDT",
                sessionId, senderAddress, FormattingTools.formatUsdt(amountSun));
        UserState userState = telegramState.getOrCreateUserState(userId);

        AmlVerification verification = null;
        try {
            verification = amlVerificationService.createPendingVerification(
                    userId, senderAddress, provider,
                    session.getChatId(), session.getMessageToUpdate());

            amlViews.showAmlRequestReceived(userState, senderAddress);

            List<UserWallet> userWallets = Collections.emptyList();
            if (user.getShowWalletsMenu()) {
                userWallets = userWalletService.getWallets(user.getTelegramId());
            }

            Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
            telegramState.updateUserState(userState.getTelegramId(), userState
                    .withState(States.MAIN_MENU)
                    .withChatId(newMenuMsg.getChatId())
                    .withMenuMessageId(newMenuMsg.getMessageId()));

            String reportLanguage = user.getLanguageCode();
            NettsAmlCreateResponse200 response = nettsRestClient.createAmlRequest(senderAddress, provider, reportLanguage);
            amlVerificationService.markProcessing(verification.getId(), response.getData());

            logger.info("Auto-AML check submitted for sender {} verification id: {}", senderAddress, verification.getId());
        } catch (NotEnoughBalanceException e) {
            logger.warn("Auto-AML session {} stopped due to insufficient balance for user {}: {}",
                    sessionId, userId, e.getMessage());
            AutoAmlSession deactivated = deactivateSession(sessionId, AutoAmlSessionDeactivationReason.INSUFFICIENT_BALANCE);
            autoAmlViews.showSessionStoppedLowBalance(userState, deactivated);
        } catch (Exception e) {
            logger.error("Auto-AML check failed for sender {} session {}: {}", senderAddress, sessionId, e.getMessage());
            if (verification != null) {
                amlVerificationService.refundVerification(verification.getId());
            }
        }
    }
}
