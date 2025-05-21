package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUsers;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class ManageGroupNewGroupView {
    // TODO: move to properties
    public static final Integer MAX_USERS_IN_GROUP = 10;

    private static final String MSG_MANAGE_GROUPS_ADD_SUCCESS = "✅ Группа успешно добавлена";

    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_LABEL = "Введите название группы";
    private static final String MSG_MANAGE_GROUPS_TXT = """
            👥 Управление группами
            Здесь вы можете управлять группами пользователей, а также просматривать и изменять их баланс
            """;

    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_USERS = "Выберете пользователей, которых хотите добавить в группу используя меню";
    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_MANAGER = """
            Выберете менеджера группы используя меню.

            Ему будет доступно управление группой, а также возможность добавлять и удалять пользователей из группы.
            """;

    private static final String MANAGE_GROUPS_SEARCH = "🔍 Поиск группы";
    private static final String MANAGE_GROUPS_ADD_NEW = "➕ Добавить группу";
    private static final String MANAGE_GROUPS_ADD_MANAGER = "👤 Выбрать менеджера группы";

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
    public void updMenuToManageGroupsMenuForManager(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_GROUPS_TXT)
                .replyMarkup(getManageGroupsMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupsAddPromptLabel(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_LABEL)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupsAddPromptUsers(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_USERS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuPromptManager(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_MANAGER)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendAddPromptUsers(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_USERS)
                .replyMarkup(getManageGroupsNewGroupPromptUsersMarkup())
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendAddPromptManager(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_MANAGER)
                .replyMarkup(getManageGroupsNewGroupPromptManagerMarkup())
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupsAddSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_GROUPS_ADD_SUCCESS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    private ReplyKeyboardMarkup getManageGroupsNewGroupPromptUsersMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(false)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text(MANAGE_GROUPS_ADD_NEW)
                                        .requestUsers(
                                                KeyboardButtonRequestUsers.builder()
                                                        .requestId("1")
                                                        .userIsBot(false)
                                                        .maxQuantity(MAX_USERS_IN_GROUP)
                                                        .build())
                                        .build()))
                .build();
    }

    private ReplyKeyboardMarkup getManageGroupsNewGroupPromptManagerMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(false)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text(MANAGE_GROUPS_ADD_MANAGER)
                                        .requestUsers(
                                                KeyboardButtonRequestUsers.builder()
                                                        .requestId("1")
                                                        .userIsBot(false)
                                                        .maxQuantity(1)
                                                        .build())
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
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.GO_BACK)
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }
}
