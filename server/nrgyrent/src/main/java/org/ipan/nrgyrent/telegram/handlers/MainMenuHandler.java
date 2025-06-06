package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Locale;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.BalanceReferralProgramRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralCommissionRepo;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.i18n.TgUserLocaleHolder;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.DepositViews;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@TransitionHandler
public class MainMenuHandler {
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final UserService userService;
    private final BalanceReferralProgramRepo balanceReferralProgramRepo;
    private final ReferralCommissionRepo referralCommissionRepo;
    private final AppUserRepo userRepo;

    private final DepositViews depositViews;

    @MatchState(state = States.CHOOSE_LANGUAGE, updateTypes = UpdateType.CALLBACK_QUERY)
    public void changeLanguage(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        String data = update.getCallbackQuery().getData();
        
        if ("en".equals(data) || "ru".equals(data)) {
            userService.setLanguage(userState.getTelegramId(), data);

            TgUserLocaleHolder.setUserLocale(Locale.of(data));
            telegramMessages.updateUserMainMenuBasedOnRole(userState, user);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MAIN_MENU));
        }
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.DEPOSIT)
    public void handleDeposit(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        depositViews.updMenuToDepositsMenu(userState, user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_REFERALS)
    public void showReferals(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());

        List<BalanceReferralProgram> byBalanceId = balanceReferralProgramRepo.findByBalanceId(user.getBalance().getId());

        if (!byBalanceId.isEmpty()) {
            BalanceReferralProgram referralProgram = byBalanceId.get(0);

            Long pendingCommissionSun = referralCommissionRepo.findSumOfAllPendingByBalanceId(referralProgram.getId());
            List<AppUser> referals = userRepo.findAllByBalRefProgId(referralProgram.getId());

            telegramMessages.updMenuToReferalSummary(userState, user, referralProgram, referals, pendingCommissionSun == null ? 0 : pendingCommissionSun);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.REFERALS));
        }
    }
}
