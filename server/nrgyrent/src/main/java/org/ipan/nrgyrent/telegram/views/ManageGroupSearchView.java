package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class ManageGroupSearchView {
    public static final String MSG_MANAGE_GROUPS_SEARCH_NO_RESULTS = "❌ Нет результатов";
    public static final String MSG_MANAGE_GROUPS_SEARCH_PAGE_RESULTS = """
            🔍 Результаты поиска
            Используйте стрелки, чтобы прокручивать результаты, или введите имя группы, чтобы найти ее.

            Выберите группу, с которой хотите работать
            """;
    private static final String MSG_MANAGE_GROUPS_TXT = """
            👥 Управление группами
            Здесь вы можете управлять группами пользователей, а также просматривать и изменять их баланс
            """;

    private static final String MANAGE_GROUPS_SEARCH = "🔍 Поиск группы";
    private static final String MANAGE_GROUPS_SEARCH_RESET = "🔄 Сбросить поиск";
    private static final String MANAGE_GROUPS_ADD_NEW = "➕ Добавить группу";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @SneakyThrows
    public void updMenuToManageGroupsMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_MANAGE_GROUPS_TXT)
                .replyMarkup(getManageGroupsMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupsSearchResult(Page<Balance> page, UserState userState) {
        String text = page.isEmpty() ? MSG_MANAGE_GROUPS_SEARCH_NO_RESULTS
                : MSG_MANAGE_GROUPS_SEARCH_PAGE_RESULTS;

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getManageGroupsSearchPageMarkup(page))
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getManageGroupsSearchPageMarkup(Page<Balance> page) {
        List<InlineKeyboardRow> groupBalances = page.getContent().stream().map(balance -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(balance.getLabel())
                            .callbackData(balance.getId().toString())
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        groupBalances.forEach(builder::keyboardRow);

        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUPS_SEARCH_RESET)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_SEARCH_RESET)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup getManageGroupsMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUPS_SEARCH)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_SEARCH)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUPS_ADD_NEW)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ADD)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }
}
