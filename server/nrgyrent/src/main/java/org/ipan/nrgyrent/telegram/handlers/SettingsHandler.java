package org.ipan.nrgyrent.telegram.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.projections.ReferralDto;
import org.ipan.nrgyrent.domain.model.projections.TransactionHistoryDto;
import org.ipan.nrgyrent.domain.model.repository.*;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.i18n.TgUserLocaleHolder;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.HistoryViews;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@TransitionHandler
public class SettingsHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserService userService;
    private final OrderRepo orderRepo;
    private final BalanceReferralProgramRepo balanceReferralProgramRepo;
    private final ReferralCommissionRepo referralCommissionRepo;
    private final AppUserRepo userRepo;
    private final BalanceRepo balanceRepo;

    private final HistoryViews historyViews;

    @MatchStates({
        @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.SETTINGS),
        @MatchState(state = States.HISTORY, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.TOPUP_HISTORY, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.SETTINGS_CHANGE_LANGUAGE, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.REFERALS, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void settingsMenu(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        telegramMessages.updateMsgToSettings(userState, user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.SETTINGS));
    }

    @MatchStates({
        @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.OPT_SHOW_WALLET_DISABLE),
        @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.OPT_SHOW_WALLET_ENABLE),
    })
    public void enableDisableOptShowWallet(UserState userState, Update update) {
        userService.setShowWalletOption(userState.getTelegramId(), InlineMenuCallbacks.OPT_SHOW_WALLET_ENABLE.equals(update.getCallbackQuery().getData()));
        AppUser user = userService.getById(userState.getTelegramId());
        telegramMessages.updateMsgToSettings(userState, user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.SETTINGS));
    }

    @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.CHANGE_LANGUAGE)
    public void changeLanguage(UserState userState, Update update) {
        telegramMessages.updateMsgToChangeLanguage(userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.SETTINGS_CHANGE_LANGUAGE));
    }

    @MatchState(state = States.SETTINGS_CHANGE_LANGUAGE, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleNewLanguage(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();

        if ("en".equals(data) || "ru".equals(data) || "uk".equals(data)) {
            userService.setLanguage(userState.getTelegramId(), data);
            AppUser user = userService.getById(userState.getTelegramId());

            TgUserLocaleHolder.setUserLocale(Locale.of(data));
            telegramMessages.updateMsgToSettings(userState, user);
            telegramState.updateUserState(userState.getTelegramId(), userState.withLanguageCode(data).withState(States.SETTINGS));
        }
    }

    @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.HISTORY)
    public void handleTransactionHistory(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        List<TransactionHistoryDto> page = user.isGroupManager()
            ? orderRepo.findAllTransactionsForManager(userState.getTelegramId(), 10)
            : orderRepo.findAllTransactions(userState.getTelegramId(), 10);
        historyViews.updMenuToHistoryMenu(page.reversed(), update.getCallbackQuery());
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.HISTORY));
    }

    @MatchState(state = States.SETTINGS, callbackData = InlineMenuCallbacks.MANAGE_REFERALS)
    public void showReferals(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());

        List<BalanceReferralProgram> byBalanceId = balanceReferralProgramRepo.findByBalanceId(user.getBalance().getId());
        BalanceReferralProgram referralProgram = byBalanceId.isEmpty() ? null : byBalanceId.get(0);

        Long pendingCommissionSun = 0L;
        List<ReferralDto> referals = Collections.emptyList();

        // for groups
        if (referralProgram != null) {
            pendingCommissionSun = referralProgram == null ? 0 : referralCommissionRepo.findSumOfAllPendingByBalanceRefProgId(referralProgram.getId()).orElse(0L);
            referals = balanceRepo.findAllByBalRefProgId(referralProgram.getId());
        }
        telegramMessages.updMenuToReferalSummary(userState, user, referralProgram, referals, pendingCommissionSun == null ? 0 : pendingCommissionSun);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.REFERALS));
    }
}
