package org.ipan.nrgyrent.telegram.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.domain.service.UserWalletService;
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
    private final UserWalletService userWalletService;

    private final DepositViews depositViews;

    @MatchState(state = States.CHOOSE_LANGUAGE, updateTypes = UpdateType.CALLBACK_QUERY)
    public void changeLanguage(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        String data = update.getCallbackQuery().getData();
        
        if ("en".equals(data) || "ru".equals(data) || "uk".equals(data)) {
            userService.setLanguage(userState.getTelegramId(), data);

            TgUserLocaleHolder.setUserLocale(Locale.of(data));

            List<UserWallet> userWallets = Collections.emptyList();
            if (user.getShowWalletsMenu()) {
                userWallets = userWalletService.getWallets(user.getTelegramId());
            }

            userState = userState.withLanguageCode(data);
            telegramMessages.updateUserMainMenuBasedOnRole(userState, user, userWallets);
            telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.MAIN_MENU));
        }
    }

    @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.DEPOSIT)
    public void handleDeposit(UserState userState, Update update) {
        AppUser user = userService.getById(userState.getTelegramId());
        depositViews.updMenuToDepositsMenu(userState, user);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.DEPOSIT));
    }
}
