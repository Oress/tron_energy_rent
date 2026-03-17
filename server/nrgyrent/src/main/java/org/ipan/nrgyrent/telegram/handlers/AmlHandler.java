package org.ipan.nrgyrent.telegram.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.domain.service.NrgConfigsService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.netts.AmlPriceCache;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.netts.dto.NettsAmlCreateResponse200;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.AmlViews;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class AmlHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final AmlVerificationService amlVerificationService;
    private final AmlVerificationRepo amlVerificationRepo;
    private final NettsRestClient nettsRestClient;
    private final NrgConfigsService nrgConfigsService;
    private final AmlPriceCache amlPriceCache;
    private final AmlViews amlViews;

    @MatchStates({
            @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.AML_CHECK),
            @MatchState(state = States.AML_HISTORY, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_ITEM_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_PROMPT_WALLET, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK)
    })
    public void openAmlMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        AmlProvider provider = nrgConfigsService.readCurrentAmlProviderConfig();
        String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), provider);
        amlViews.showAmlMenu(userState, estimatedPrice);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AML_CHECK)
    public void promptWalletAddress(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        AmlProvider provider = nrgConfigsService.readCurrentAmlProviderConfig();
        String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), provider);
        amlViews.showAmlPromptWallet(userState, user.getBalanceToUse(), estimatedPrice);
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

    @MatchState(state = States.AML_HISTORY, updateTypes = UpdateType.CALLBACK_QUERY)
    public void showHistoryItem(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long id = InlineMenuCallbacks.getAmlViewItemId(data);
        if (id == null) {
            return;
        }
        amlVerificationRepo.findById(id).ifPresent(v -> {
            amlViews.showAmlVerificationReport(userState, v);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_ITEM_PREVIEW));
        });
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

        AmlProvider provider = nrgConfigsService.readCurrentAmlProviderConfig();
        AmlVerification verification;
        try {
            verification = amlVerificationService.createPendingVerification(
                    userState.getTelegramId(), walletAddress, provider,
                    userState.getChatId(), userState.getMenuMessageId());
        } catch (NotEnoughBalanceException e) {
            AppUser user = userService.getById(userState.getTelegramId());
            String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), provider);
            amlViews.showAmlInsufficientBalance(userState, user.getBalanceToUse(), estimatedPrice);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
            return;
        }

        try {
            NettsAmlCreateResponse200 response = nettsRestClient.createAmlRequest(walletAddress, provider);
            amlVerificationService.markProcessing(verification.getId(), response.getData());
            logger.info("AML request submitted for wallet {} with verification id: {}", walletAddress, verification.getId());
        } catch (Exception e) {
            logger.error("Failed to submit AML request for wallet {}: {}", walletAddress, e.getMessage());
            amlVerificationService.refundVerification(verification.getId());
        }

        amlViews.showAmlRequestReceived(userState, walletAddress);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_SUCCESS));
    }

    private String computeEstimatedPriceTrx(Tariff tariff, AmlProvider provider) {
        if (tariff == null || tariff.getAmlCheckPercentage() == null) {
            return "N/A";
        }
        AmlPriceCache.AmlPrice cachedPrice = amlPriceCache.getPrice(provider);
        if (cachedPrice == null || cachedPrice.getPriceTrx() == null) {
            return "N/A";
        }
        return AmlVerificationService.computeAmlPriceTrx(cachedPrice.getPriceTrx(), tariff.getAmlCheckPercentage()).toPlainString();
    }
}
