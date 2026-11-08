package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.service.UsdtExchangeSettingsService;
import org.ipan.nrgyrent.telegram.*;
import org.ipan.nrgyrent.telegram.mapdb.UserStateInMem;
import org.ipan.nrgyrent.telegram.state.*;
import org.ipan.nrgyrent.telegram.statetransitions.*;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsdtExchangeAdminHandlerTest {
    private final UsdtExchangeSettingsService settings = mock(UsdtExchangeSettingsService.class);
    private final AdminViews views = mock(AdminViews.class);
    private final TelegramState state = mock(TelegramState.class);
    private final UsdtExchangeAdminHandler handler = new UsdtExchangeAdminHandler(settings, views, state);
    private final UserState user = UserStateInMem.builder().telegramId(42L).role(UserRole.ADMIN).build();

    @Test void opensCurrentSelectionAndEntersProviderMenu() {
        when(settings.currentProvider()).thenReturn(UsdtExchangeProvider.BYBIT);
        handler.show(user, new Update());
        verify(views).usdtExchangeProvider(user, UsdtExchangeProvider.BYBIT, false);
        verify(state).updateUserState(eq(42L), argThat(s -> s.getState() == States.ADMIN_USDT_EXCHANGE_PROVIDER));
    }

    @Test void selectsFixedFloatWhenConfigured() {
        when(settings.isFixedFloatConfigured()).thenReturn(true);
        handler.selectFixedFloat(user, new Update());
        verify(settings).updateProvider(UsdtExchangeProvider.FIXEDFLOAT);
    }

    @Test void cannotSelectUnconfiguredFixedFloat() {
        handler.selectFixedFloat(user, new Update());
        verify(settings, never()).updateProvider(any());
    }

    @Test void canSwitchBackToBybit() {
        handler.selectBybit(user, new Update());
        verify(settings).updateProvider(UsdtExchangeProvider.BYBIT);
    }

    @Test void everyProviderCallbackRejectsNonAdminsThroughRealDispatcher() {
        var registry = new StateHandlerRegistry();
        for (var method : UsdtExchangeAdminHandler.class.getDeclaredMethods()) {
            var annotation = method.getAnnotation(MatchState.class);
            if (annotation == null) continue;
            registry.register(annotation, method, handler);
            Update update = new Update();
            CallbackQuery query = new CallbackQuery();
            query.setData(annotation.callbackData());
            update.setCallbackQuery(query);
            var matcher = registry.getHandlers(annotation.state(), UpdateType.CALLBACK_QUERY).stream()
                    .filter(m -> m.getMethod().equals(method)).findFirst().orElseThrow();
            for (UserRole role : UserRole.values()) {
                var requester = UserStateInMem.builder().role(role).build();
                assertEquals(role == UserRole.ADMIN, matcher.matches(requester, update));
            }
        }
    }
}
