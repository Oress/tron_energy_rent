package org.ipan.nrgyrent.telegram.views.tariffs;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class TariffsSearchView {
    public static final String OPEN_TARIFF = "/tariff/";

    private static final String MSG_MANAGE_TARIFFS_SEARCH_NO_RESULTS = "❌ Нет результатов";
    private static final String MSG_MANAGE_TARIFFS_SEARCH_PAGE_RESULTS = """
            🔍 Результаты поиска
            Используйте стрелки, чтобы прокручивать результаты, или введите имя тарифа, чтобы найти его.
            """;

    private static final String NEXT_PAGE = "➡️";
    private static final String PREV_PAGE = "⬅️";
    private static final String MANAGE_TARIFFS_SEARCH_RESET = "🔄 Сбросить поиск";

    private final TelegramClient tgClient;

    @SneakyThrows
    public void updMenuToTariffSearchResult(Page<Tariff> page, UserState userState) {
        String text = page.isEmpty() ? MSG_MANAGE_TARIFFS_SEARCH_NO_RESULTS
                : MSG_MANAGE_TARIFFS_SEARCH_PAGE_RESULTS;

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getTariffSearchPageMarkup(page))
                .build();
        tgClient.execute(message);
    }

    public String openTariffRequest(Long tariffId) {
        return OPEN_TARIFF + tariffId;
    }

    private InlineKeyboardMarkup getTariffSearchPageMarkup(Page<Tariff> page) {
        List<InlineKeyboardRow> tariffs = page.getContent().stream().map(tariff -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(tariff.getLabel())
                            .callbackData(openTariffRequest(tariff.getId()))
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        tariffs.forEach(builder::keyboardRow);

        builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_TARIFFS_SEARCH_RESET)
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_SEARCH_RESET)
                                        .build()));
        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(PREV_PAGE)
                                .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(NEXT_PAGE)
                                .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_NEXT_PAGE)
                                .build());
            }
            builder.keyboardRow(new InlineKeyboardRow(buttons));
        }

        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.GO_BACK)
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }
}
