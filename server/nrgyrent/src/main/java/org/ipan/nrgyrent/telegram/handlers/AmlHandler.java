package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;

import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlCreateResponse200;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AmlViews;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class AmlHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final AmlVerificationService amlVerificationService;
    private final AmlVerificationRepo amlVerificationRepo;
    private final NettsRestClient nettsRestClient;
    private final AmlViews amlViews;
    private final CommonLabels commonLabels;
    private final TelegramClient tgClient;

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.AML_CHECK)
    public void openAmlMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        amlViews.showAmlMenu(userState, user.getTariffToUse());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AML_CHECK)
    public void promptWalletAddress(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        amlViews.showAmlPromptWallet(userState, user.getBalanceToUse(), user.getTariffToUse());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_PROMPT_WALLET));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AML_HISTORY)
    public void showHistory(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        Balance balance = user.getBalanceToUse();
        List<AmlVerification> history = amlVerificationRepo.findAllByBalanceIdOrderByCreatedAtDesc(balance.getId());
        amlViews.showAmlHistory(userState, history);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_HISTORY));
    }

    @MatchState(state = States.AML_HISTORY, callbackData = InlineMenuCallbacks.GO_BACK)
    public void backFromHistoryToAmlMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        amlViews.showAmlMenu(userState, user.getTariffToUse());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @MatchState(state = States.AML_PROMPT_WALLET, callbackData = InlineMenuCallbacks.GO_BACK)
    public void backFromPromptToAmlMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        amlViews.showAmlMenu(userState, user.getTariffToUse());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @MatchState(state = States.AML_PROMPT_WALLET, updateTypes = UpdateType.MESSAGE)
    public void handleWalletInput(UserState userState, Update update) {
        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        String walletAddress = message.getText().trim();
        if (!WalletTools.isValidTronAddress(walletAddress)) {
            logger.warn("Invalid tron address for AML check: {} user: {}", walletAddress, userState.getTelegramId());
            return;
        }

        AmlVerification verification;
        try {
            verification = amlVerificationService.createPendingVerification(userState.getTelegramId(), walletAddress, AmlProvider.BITOK);
        } catch (NotEnoughBalanceException e) {
            AppUser user = userService.getById(userState.getTelegramId());
            amlViews.showAmlInsufficientBalance(userState, user.getBalanceToUse(), user.getTariffToUse());
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
            return;
        }

        try {
            NettsAmlCreateResponse200 response = nettsRestClient.createAmlRequest(walletAddress, AmlProvider.BITOK);
            String clientOrderId = response.getData().getClientOrderId();
            amlVerificationService.markProcessing(verification.getId(), clientOrderId);
            logger.info("AML request submitted for wallet {} with orderId: {}", walletAddress, clientOrderId);
        } catch (Exception e) {
            logger.error("Failed to submit AML request for wallet {}: {}", walletAddress, e.getMessage());
            amlVerificationService.refundVerification(verification.getId());
        }

        sendRequestReceivedNotification(userState, walletAddress);
        AppUser user = userService.getById(userState.getTelegramId());
        amlViews.showAmlMenu(userState, user.getTariffToUse());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @SneakyThrows
    private void sendRequestReceivedNotification(UserState userState, String walletAddress) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("OK")
                                .callbackData(InlineMenuCallbacks.NTFN_OK)
                                .build()))
                .build();
        SendMessage sendMessage = SendMessage.builder()
                .chatId(userState.getChatId())
                .text(commonLabels.amlRequestReceived(userState.getLocaleOrDefault(), walletAddress))
                .parseMode("MARKDOWN")
                .replyMarkup(markup)
                .build();
        tgClient.execute(sendMessage);
    }
}
