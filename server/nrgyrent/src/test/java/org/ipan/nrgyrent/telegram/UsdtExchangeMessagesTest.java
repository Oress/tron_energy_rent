package org.ipan.nrgyrent.telegram;

import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;
import org.ipan.nrgyrent.domain.service.UsdtExchangeSettingsService;
import org.ipan.nrgyrent.telegram.i18n.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsdtExchangeMessagesTest {
    @ParameterizedTest @ValueSource(strings = {"en", "ru", "uk"})
    void personalAndGroupMenusFollowSelectedProviderWithoutUnresolvedPlaceholders(String language) {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        var locale = mock(TgUserLocaleProvider.class);
        when(locale.getUserLocale()).thenReturn(Locale.forLanguageTag(language));
        var settings = mock(UsdtExchangeSettingsService.class);
        var labels = new DepositLabels(source, locale, settings);
        when(settings.currentProvider()).thenReturn(UsdtExchangeProvider.FIXEDFLOAT);
        for (String message : new String[]{labels.depositPersonal("address", "100"), labels.depositGroup("address", "100")}) {
            assertTrue(message.contains("ff.io"));
            assertTrue(message.contains(language.equals("en") ? "1.50" : "1,50"));
            assertTrue(message.contains(language.equals("en") ? "0.5%" : "0,5%"));
            assertFalse(message.contains("{2}"));
            assertFalse(message.contains("Bybit"));
            assertTrue(message.contains("The exchange service is provided by FixedFloat. Creating an order confirms your agreement with the [FixedFloat rules](https://ff.io/terms-of-service)."));
        }
        assertFalse(labels.depositPersonalTrxOnly("address", "100").contains("FixedFloat"));
        assertFalse(labels.depositGroupTrxOnly("address", "100").contains("FixedFloat"));
        when(settings.currentProvider()).thenReturn(UsdtExchangeProvider.BYBIT);
        assertTrue(labels.depositPersonal("address", "100").contains("Bybit"));
        assertFalse(labels.depositPersonal("address", "100").contains("ff.io"));
        assertFalse(labels.depositPersonal("address", "100").contains("FixedFloat"));
        String receipt = source.getMessage("notification.topup_usdt_fixedfloat",
                new Object[]{"10", "0.3", "28"}, Locale.forLanguageTag(language));
        assertTrue(receipt.contains("28"));
        assertFalse(receipt.contains("{2}"));
    }
}
