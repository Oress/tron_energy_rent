package org.ipan.nrgyrent.telegram;

import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.mapdb.UserStateInMem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixedFloatReviewMessagesTest {
    @ParameterizedTest @ValueSource(strings = {"en", "ru", "uk"})
    void sendsLocalizedSupportInstructionsWithoutExposingTokenOrParsingExternalMarkup(String language) throws Exception {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        var client = mock(TelegramClient.class);
        var messages = new TelegramMessages(client, null, null,
                new CommonLabels(source, null), null, null, null);
        var state = UserStateInMem.builder().telegramId(99L).chatId(999L).languageCode(language).build();
        var deposit = new DepositTransaction();
        deposit.setExchangeOrderId("ABC_123[external]");
        deposit.setExchangeOrderToken("SECRET_ORDER_TOKEN");
        deposit.setExchangeUsdtTx("usdt-tx");
        deposit.setExchangeDepositAddress("ff-deposit-address");
        deposit.setExchangePayoutAddress("collection-address");

        messages.sendFixedFloatReviewNotification(state, deposit);

        var sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(client).execute(sent.capture());
        assertEquals("999", sent.getValue().getChatId());
        assertNull(sent.getValue().getParseMode());
        String text = sent.getValue().getText();
        assertTrue(text.contains("ABC_123[external]"));
        assertTrue(text.contains("usdt-tx"));
        assertTrue(text.contains("ff-deposit-address"));
        assertTrue(text.contains("collection-address"));
        assertTrue(text.contains("api@fixedfloat.com"));
        assertTrue(text.contains("compliance@fixedfloat.com"));
        assertFalse(text.contains("SECRET_ORDER_TOKEN"));
        assertFalse(text.matches("(?s).*\\{[0-9]+}.*"));
        assertTrue(text.startsWith(switch (language) {
            case "ru" -> "⚠️ Обмен FixedFloat требует проверки";
            case "uk" -> "⚠️ Обмін FixedFloat потребує перевірки";
            default -> "⚠️ FixedFloat exchange requires review";
        }));
    }
}
