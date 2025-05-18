package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
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
public class ManageGroupActionsView {
    private static final String MANAGE_GROUP_ACTION_VIEW_USERS = "👥 Просмотреть пользователей";
    private static final String MANAGE_GROUP_ACTION_SET_MANAGER = "👤 Установить менеджера группы";
    private static final String MANAGE_GROUP_ACTION_ADD_USERS = "➕ Добавить пользователей";
    private static final String MANAGE_GROUP_ACTION_REMOVE_USERS = "➖ Удалить пользователей";
    private static final String MANAGE_GROUP_ACTION_RENAME_GROUP = "✏️ Переименовать группу";
    private static final String MANAGE_GROUP_ACTION_DEACTIVATE_GROUP = "❌ Деактивировать группу";

    private static final String MSG_DELETE_GROUP_WARNING = "⚠️ Вы уверены, что хотите деактивировать группу?";
    private static final String MSG_GROUP_DELETED = "✅ Группа успешно деактивирована.";
    private static final String MSG_GROUP_PROMPT_NEW_LABEL = "Введите новое название группы";
    private static final String MSG_GROUP_RENAMED = "✅ Группа успешно переименована.";

    private static final String NO = "❌ Нет";
    private static final String YES = "✅ Да";

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
    public void groupDeleted(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_GROUP_DELETED)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void groupRenamed(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_RENAMED)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewGroupLabel(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_GROUP_PROMPT_NEW_LABEL)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void reviewGroupUsers(CallbackQuery callbackQuery, List<AppUser> users) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getUsersList(users))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void confirmDeactivateGroupMsg(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_DELETE_GROUP_WARNING)
                .replyMarkup(confirmDeleteGroupMarkup())
                .build();
        tgClient.execute(message);
    }

    public InlineKeyboardMarkup confirmDeleteGroupMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(NO)
                                        .callbackData(InlineMenuCallbacks.CONFIRM_NO)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(YES)
                                        .callbackData(InlineMenuCallbacks.CONFIRM_YES)
                                        .build()))
                .build();
    }

    private String getBalanceDescription(Balance balance) {
        return String.format("""
                ⚙️ Действия с группой

                ID: %s
                Название: %s
                Создана: %s
                Активна: %s

                Кошелек: %s
                Баланс: %s TRX
                """,
                balance.getId(),
                balance.getLabel(),
                balance.getCreatedAt().toString(),
                balance.getIsActive() ? "✅" : "❌",
                balance.getDepositAddress(),
                FormattingTools.formatBalance(balance.getSunBalance()));
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

    private String getUsersList(List<AppUser> users) {
        return """
                👥 Список пользователей группы

                %s
                """
                .formatted(users.stream()
                        .map(user -> String.format("ID: %s, Логин: %s, Имя: %s", user.getTelegramId(),
                                user.getTelegramUsername(), user.getTelegramFirstName()))
                        .collect(Collectors.joining("\n")));
    }
}
