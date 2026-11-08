package org.ipan.nrgyrent.telegram.handlers;

import lombok.RequiredArgsConstructor;
import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;
import org.ipan.nrgyrent.domain.service.UsdtExchangeSettingsService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.telegram.telegrambots.meta.api.objects.Update;

@TransitionHandler
@RequiredArgsConstructor
public class UsdtExchangeAdminHandler {
    private final UsdtExchangeSettingsService settings;
    private final AdminViews views;
    private final TelegramState state;

    @MatchState(forAdmin = true, state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_USDT_EXCHANGE)
    public void show(UserState user, Update update) {
        views.usdtExchangeProvider(user, settings.currentProvider(), settings.isFixedFloatConfigured());
        state.updateUserState(user.getTelegramId(), user.withState(States.ADMIN_USDT_EXCHANGE_PROVIDER));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_USDT_EXCHANGE_PROVIDER, callbackData = InlineMenuCallbacks.USDT_EXCHANGE_BYBIT)
    public void selectBybit(UserState user, Update update) {
        settings.updateProvider(UsdtExchangeProvider.BYBIT);
        show(user, update);
    }

    @MatchState(forAdmin = true, state = States.ADMIN_USDT_EXCHANGE_PROVIDER, callbackData = InlineMenuCallbacks.USDT_EXCHANGE_FIXEDFLOAT)
    public void selectFixedFloat(UserState user, Update update) {
        if (settings.isFixedFloatConfigured()) settings.updateProvider(UsdtExchangeProvider.FIXEDFLOAT);
        show(user, update);
    }
}
