package org.ipan.nrgyrent.telegram.views.tariffs;

import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
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
public class TariffsManageView {
    private static final String MSG_MANAGE_TARIFS_TXT = """
            📊 Управление тарифами
            Здесь вы можете управлять тарифами пользователей.
            """;

    private static final String MANAGE_TARIFS_SEARCH = "🔍 Поиск тарифов";
    private static final String MANAGE_TARIFS_ADD_NEW = "➕ Добавить тариф";

    private final TelegramClient tgClient;

    @SneakyThrows
    public void updMenuToTariffsManageMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_TARIFS_TXT)
                .replyMarkup(getManageTariffsMarkup())
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getManageTariffsMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_TARIFS_SEARCH)
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_SEARCH)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_TARIFS_ADD_NEW)
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ADD)
                                        .build()))
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
