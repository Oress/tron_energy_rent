package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Locale;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.projections.TransactionHistoryDto;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
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

    private final HistoryViews historyViews;

    @MatchStates({
        @MatchState(state = States.MAIN_MENU, callbackData = InlineMenuCallbacks.SETTINGS),
        @MatchState(state = States.HISTORY, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.SETTINGS_CHANGE_LANGUAGE, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void settingsMenu(UserState userState, Update update) {
        telegramMessages.updateMsgToSettings(userState);
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

        if ("en".equals(data) || "ru".equals(data)) {
            userService.setLanguage(userState.getTelegramId(), data);

            TgUserLocaleHolder.setUserLocale(Locale.of(data));
            telegramMessages.updateMsgToSettings(userState);
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
}
