package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.BalanceTools;
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
    private static final Integer MAX_USERS_IN_GROUP = 10;

    private static final String MSG_MANAGE_GROUPS_ADD_SUCCESS = "✅ Группа успешно добавлена";

    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_LABEL = "Введите название группы";
    private static final String MSG_MANAGE_GROUPS_TXT = """
            👥 Управление группами
            Здесь вы можете управлять группами пользователей, а также просматривать и изменять их баланс
            """;

    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_USERS = "Выберете пользователей, которых хотите добавить в группу используя меню";

    private static final String MANAGE_GROUP_ACTION_VIEW_USERS = "👥 Просмотреть пользователей";
    private static final String MANAGE_GROUP_ACTION_SET_MANAGER = "👤 Установить менеджера группы";
    private static final String MANAGE_GROUP_ACTION_ADD_USERS = "➕ Добавить пользователей";
    private static final String MANAGE_GROUP_ACTION_REMOVE_USERS = "➖ Удалить пользователей";
    private static final String MANAGE_GROUP_ACTION_RENAME_GROUP = "✏️ Переименовать группу";
    private static final String MANAGE_GROUP_ACTION_DEACTIVATE_GROUP = "❌ Деактивировать группу";

    private static final String MANAGE_GROUPS_SEARCH = "🔍 Поиск группы";
    private static final String MANAGE_GROUPS_ADD_NEW = "➕ Добавить группу";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @SneakyThrows
    public void updMenuToManageGroupActionsMenu(CallbackQuery callbackQuery, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getBalanceDescription(balance))
                .replyMarkup(getManageGroupActionsMarkup())
                .build();
        tgClient.execute(message);
    }

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
    public void updMenuToManageGroupsAddPromptLabel(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_LABEL)
                .replyMarkup(getManageGroupsNewGroupMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message updMenuToManageGroupsAddPromptUsers(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(MSG_MANAGE_GROUPS_ADD_PROMPT_USERS)
                .replyMarkup(getManageGroupsNewGroupPromptUsersMarkup())
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
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getManageGroupsNewGroupMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build())

                )
                .build();
    }

    private ReplyKeyboardMarkup getManageGroupsNewGroupPromptUsersMarkup() {
        return ReplyKeyboardMarkup
                .builder()
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

    private InlineKeyboardMarkup getManageGroupActionsMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_SET_MANAGER)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_SET_MANAGER)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_RENAME_GROUP)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_RENAME)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_VIEW_USERS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_ADD_USERS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_REMOVE_USERS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_DEACTIVATE_GROUP)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DEACTIVATE)
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

    private String getBalanceDescription(Balance balance) {
        return String.format("""
                ⚙️ Действия с группой

                ID: %s
                Название: %s
                Создана: %s

                Кошелек: %s
                Баланс: %s TRX
                """,
                balance.getLabel(),
                balance.getId(),
                balance.getCreatedAt().toString(),
                balance.getDepositAddress(),
                BalanceTools.formatBalance(balance.getSunBalance()));
    }
}
