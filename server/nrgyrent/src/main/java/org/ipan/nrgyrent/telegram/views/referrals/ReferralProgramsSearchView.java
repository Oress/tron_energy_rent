package org.ipan.nrgyrent.telegram.views.referrals;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class ReferralProgramsSearchView {
    public static final String OPEN_REF_PROGRAM = "/ref_program/";

    private static final String NEXT_PAGE = "➡️";
    private static final String PREV_PAGE = "⬅️";

    private final TelegramClient tgClient;

    public void updMenuToSearchResult(Page<ReferralProgram> page, UserState userState) {
        String text = page.isEmpty() 
                ? "❌ Нет результатов"
                : """
            🔍 Результаты поиска
            Используйте стрелки, чтобы прокручивать результаты, или введите имя чтобы искать по названию.
            """;

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getTariffSearchPageMarkup(page))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updMenuToTariffSearchResult userstate {}", userState, e);
        }
    }

    public String openTariffRequest(Long tariffId) {
        return OPEN_REF_PROGRAM + tariffId;
    }

    private InlineKeyboardMarkup getTariffSearchPageMarkup(Page<ReferralProgram> page) {
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
                                        .text("🔄 Сбросить поиск")
                                        .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_SEARCH_RESET)
                                        .build()));
        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(PREV_PAGE)
                                .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(NEXT_PAGE)
                                .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_NEXT_PAGE)
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
