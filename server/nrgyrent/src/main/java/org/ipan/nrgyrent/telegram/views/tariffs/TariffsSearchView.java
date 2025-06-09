package org.ipan.nrgyrent.telegram.views.tariffs;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
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
public class TariffsSearchView {
    public static final String OPEN_TARIFF = "/tariff/";

    private final TelegramClient tgClient;
    private final CommonLabels commonLabels;

    public void updMenuToTariffSearchResult(Page<Tariff> page, UserState userState) {
        String text = page.isEmpty() ? commonLabels.searchNoResults()
                : commonLabels.searchResults();

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
                                        .text(commonLabels.searchReset())
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_SEARCH_RESET)
                                        .build()));
        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchPrevPage())
                                .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchNextPage())
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
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.goBack())
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }
}
