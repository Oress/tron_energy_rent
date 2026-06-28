package org.ipan.nrgyrent.telegram.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.AmlCheckSubmitter;
import org.ipan.nrgyrent.domain.exception.NotEnoughBalanceException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.AmlVerificationRepo;
import org.ipan.nrgyrent.domain.service.AmlVerificationService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
import org.ipan.nrgyrent.netts.AmlPriceCache;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@TransitionHandler
@Slf4j
public class AmlHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final AmlVerificationRepo amlVerificationRepo;
    private final AmlPriceCache amlPriceCache;
    private final AmlCheckSubmitter amlCheckSubmitter;
    private final AmlViews amlViews;
    private final TelegramMessages telegramMessages;
    private final UserWalletService userWalletService;

    @MatchStates({
            @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.AML_CHECK),
            @MatchState(state = States.AML_HISTORY, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_ITEM_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_PROMPT_WALLET, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AML_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.SETTINGS_AML_PROVIDER, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.AUTO_AML_MENU, callbackData = InlineMenuCallbacks.GO_BACK)
    })
    public void openAmlMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        AmlProvider provider = user.getAmlProvider();
        String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), provider);
        List<UserWallet> userWallets = Collections.emptyList();
        if (user.getShowWalletsMenu()) {
            userWallets = userWalletService.getWallets(user.getTelegramId());
        }
        amlViews.showAmlMenu(userState, estimatedPrice, provider, userWallets);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.SETTINGS_AML_PROVIDER)
    public void showAmlProviderSelect(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        telegramMessages.updateMsgToAmlProviderSelect(userState, user.getAmlProvider());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.SETTINGS_AML_PROVIDER));
    }

    @MatchStates({
            @MatchState(state = States.SETTINGS_AML_PROVIDER, callbackData = InlineMenuCallbacks.SETTINGS_AML_PROVIDER_ELLIPTIC),
            @MatchState(state = States.SETTINGS_AML_PROVIDER, callbackData = InlineMenuCallbacks.SETTINGS_AML_PROVIDER_BITOK),
            @MatchState(state = States.SETTINGS_AML_PROVIDER, callbackData = InlineMenuCallbacks.SETTINGS_AML_PROVIDER_BOTH),
    })
    public void handleAmlProviderSelect(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        AmlProvider provider;
        if (InlineMenuCallbacks.SETTINGS_AML_PROVIDER_ELLIPTIC.equals(data)) {
            provider = AmlProvider.ELLIPTIC;
        } else if (InlineMenuCallbacks.SETTINGS_AML_PROVIDER_BITOK.equals(data)) {
            provider = AmlProvider.BITOK;
        } else {
            provider = AmlProvider.ELLIPTIC_AND_BITOK;
        }
        userService.setAmlProvider(userState.getTelegramId(), provider);
        telegramMessages.updateMsgToAmlProviderSelect(userState, provider);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.SETTINGS_AML_PROVIDER));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AML_CHECK)
    public void promptWalletAddress(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        AmlProvider provider = user.getAmlProvider();
        String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), provider);
        amlViews.showAmlPromptWallet(userState, user.getBalanceToUse(), estimatedPrice);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_PROMPT_WALLET));
    }

    @MatchState(state = States.AML_MENU, callbackData = InlineMenuCallbacks.AML_HISTORY)
    public void showHistory(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        Balance balance = user.getBalanceToUse();
        List<AmlVerification> history = amlVerificationRepo.findTop15ByBalanceIdOrderByCreatedAtDesc(balance.getId());
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

        AppUser user = userService.getById(userState.getTelegramId());
        submitAmlCheck(userState, user, walletAddress);
    }

    @MatchState(state = States.AML_MENU, updateTypes = UpdateType.CALLBACK_QUERY)
    public void checkSavedWallet(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long walletId = InlineMenuCallbacks.getWalletIdForAmlCheck(data);
        if (walletId == null) {
            return;
        }

        AppUser user = userService.getById(userState.getTelegramId());
        String walletAddress = userWalletService.getWallets(user.getTelegramId()).stream()
                .filter(w -> w.getId().equals(walletId))
                .map(UserWallet::getAddress)
                .findFirst()
                .orElse(null);
        if (walletAddress == null) {
            logger.warn("Wallet {} not found for AML check user: {}", walletId, userState.getTelegramId());
            return;
        }

        submitAmlCheck(userState, user, walletAddress);
    }

    private void submitAmlCheck(UserState userState, AppUser user, String walletAddress) {
        try {
            int submitted = amlCheckSubmitter.submitChecks(
                    user, walletAddress, userState.getChatId(), userState.getMenuMessageId());
            if (submitted == 0) {
                return;
            }

            amlViews.showAmlRequestReceived(userState, walletAddress);

            List<UserWallet> userWallets = Collections.emptyList();
            if (user.getShowWalletsMenu()) {
                userWallets = userWalletService.getWallets(user.getTelegramId());
            }

            Message newMenuMsg = telegramMessages.sendUserMainMenuBasedOnRole(userState, userState.getChatId(), user, userWallets);
            telegramState.updateUserState(userState.getTelegramId(), userState
                    .withState(States.MAIN_MENU)
                    .withChatId(newMenuMsg.getChatId())
                    .withMenuMessageId(newMenuMsg.getMessageId()));
        } catch (NotEnoughBalanceException e) {
            String estimatedPrice = computeEstimatedPriceTrx(user.getTariffToUse(), user.getAmlProvider());
            amlViews.showAmlInsufficientBalance(userState, user.getBalanceToUse(), estimatedPrice);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.AML_MENU));
        }
    }

    private String computeEstimatedPriceTrx(Tariff tariff, AmlProvider provider) {
        if (AmlProvider.ELLIPTIC_AND_BITOK.equals(provider)) {
            BigDecimal elliptic = computeProviderPriceTrx(tariff, AmlProvider.ELLIPTIC);
            BigDecimal bitok = computeProviderPriceTrx(tariff, AmlProvider.BITOK);
            if (elliptic == null || bitok == null) {
                return "N/A";
            }
            return elliptic.add(bitok).toPlainString();
        }
        BigDecimal price = computeProviderPriceTrx(tariff, provider);
        return price != null ? price.toPlainString() : "N/A";
    }

    private BigDecimal computeProviderPriceTrx(Tariff tariff, AmlProvider provider) {
        if (tariff == null || tariff.getAmlCheckPercentage() == null) {
            return null;
        }
        AmlPriceCache.AmlPrice cachedPrice = amlPriceCache.getPrice(provider);
        if (cachedPrice == null || cachedPrice.getPriceTrx() == null) {
            return null;
        }
        return AmlVerificationService.computeAmlPriceTrx(cachedPrice.getPriceTrx(), tariff.getAmlCheckPercentage());
    }
}
