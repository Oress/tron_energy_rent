package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
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
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class ManageGroupActionsView {
    private static final String MANAGE_GROUP_ACTION_VIEW_USERS = "👥 Просмотреть пользователей";
    private static final String MANAGE_GROUP_ACTION_SET_MANAGER = "👤 Установить менеджера группы";
    private static final String MANAGE_GROUP_ACTION_ADJUST_BALANCE_MANUALLY = "💰 Изменить баланс вручную";
    private static final String MANAGE_GROUP_ACTION_ADD_USERS = "➕ Добавить пользователей";
    private static final String MANAGE_GROUP_ACTION_REMOVE_USERS = "➖ Удалить пользователей";
    private static final String MANAGE_GROUP_ACTION_RENAME_GROUP = "✏️ Переименовать группу";
    private static final String MANAGE_GROUP_ACTION_CHANGE_TARIFF = "✏️ Изменить тариф группы";
    private static final String MANAGE_GROUP_ACTION_DEACTIVATE_GROUP = "❌ Деактивировать группу";

    private static final String MSG_DELETE_GROUP_WARNING = "⚠️ Вы уверены, что хотите деактивировать группу?";
    private static final String MSG_GROUP_DELETED = "✅ Группа успешно деактивирована.";
    private static final String MSG_GROUP_PROMPT_NEW_LABEL = "Введите новое название группы";
    private static final String MSG_GROUP_PROMPT_NEW_BALANCE = "Введите новый баланс группы (в TRX)";
    private static final String MSG_GROUP_PROMPT_NEW_USERS = """
    Добавьте пользователей в группу, используя меню
    (Пользователи должны быть зарегестированы в боте)
    """;
    private static final String MSG_GROUP_PROMPT_REMOVE_USERS = "Удалите пользователей из группы, используя меню";
    private static final String MSG_GROUP_RENAMED = "✅ Группа успешно переименована.";
    private static final String MSG_USER_TARIFF_CHANGED = "✅ Тариф группы успешно изменен.";
    private static final String MSG_GROUP_TOO_SHORT = "❌ Название группы слишком короткое. Минимум 3 символа. Попробуйте снова.";
    private static final String MSG_GROUP_BALANCE_ADJUSTED = "✅ Баланс группы успешно изменен.";
    private static final String MSG_GROUP_USERS_ADDED = "✅ Пользователи успешно добавлены в группу.";
    private static final String MSG_GROUP_USERS_REMOVED = "✅ Пользователи успешно удалены из группы.";
    private static final String MANAGE_GROUPS_CHANGE_MANAGER = "👤 Выбрать менеджера группы";
    private static final String MANAGE_GROUPS_MANAGER_CHANGED = "✅ Менеджер группы успешно изменен.";

    private static final String MSG_MANAGE_GROUPS_ADD_PROMPT_MANAGER = """
            Выберете менеджера группы используя меню.
            Старый менеджер останется учасником группы.
            Нельзя выбирать участников других груп в качествве менеджера.

            Ему будет доступна возможность добавлять и удалять пользователей из группы.
            """;

    private static final String NO = "❌ Нет";
    private static final String YES = "✅ Да";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    public void somethingWentWrong(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Произошла ошибка. Попробуйте снова.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not somethingWentWrong userstate {}", userState, e);
        }
    }

    public void userAlreadyManagesAnotherGroup(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Выбраные пользователи уже управляет другой группой.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not userAlreadyManagesAnotherGroup userstate {}", userState, e);
        }
    }

    public void cannotRemoveManager(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Пользователь не может быть удален из группы, так как он является менеджером.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not cannotRemoveManager userstate {}", userState, e);
        }
    }

    public void someUsersAreNotRegistered(UserState userState, List<TgUserId> notRegisteredUsers) {
        String list = notRegisteredUsers.stream().map(u -> FormattingTools.formatUserLink(u)).collect(Collectors.joining("\n"));

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .parseMode("MARKDOWN")
                .text("""
                ❌ Некоторые из выбраных пользователей не зарегистрированы:
                %s
                
                Попробуйте снова.""".formatted(list))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not someUsersAreNotRegistered userstate {}", userState, e);
        }
    }

    public void userBelongsToAnotherGroup(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Выбраные пользователи уже пренадлежат другой группе.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not someUsersAreNotRegistered userstate {}", userState, e);
        }
    }

    public void groupBalanceIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Баланс группы не может быть отрицательным. Попробуйте снова.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupBalanceIsNegative userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void updMenuManagerChanged(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MANAGE_GROUPS_MANAGER_CHANGED)
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
    public Message sendPromptManager(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text("Выберете пользователя")
                .replyMarkup(getManageGroupsNewGroupPromptManagerMarkup())
                .build();
        return tgClient.execute(message);
    }

    private ReplyKeyboardMarkup getManageGroupsNewGroupPromptManagerMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(false)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text("Выбрать пользователя")
                                        .requestUsers(
                                                KeyboardButtonRequestUsers.builder()
                                                        .requestId("1")
                                                        .userIsBot(false)
                                                        .maxQuantity(1)
                                                        .requestName(true)
                                                        .requestUsername(true)
                                                        .build())
                                        .build()))
                .build();
    }

    @SneakyThrows
    public void updMenuToManageGroupActionsMenu(UserState userState, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(balance))
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(getManageGroupActionsMarkup(true, balance.getIsActive()))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupActionsMenuForManager(UserState userState, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(balance))
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(getManageGroupActionsMarkupForManager())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void groupDeleted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_DELETED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
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
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void userTariffChanged(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_USER_TARIFF_CHANGED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void groupNameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_TOO_SHORT)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupNameIsTooShort userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void groupBalanceAdjusted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_BALANCE_ADJUSTED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void groupUsersAdded(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_USERS_ADDED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void groupUsersRemoved(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_USERS_REMOVED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuPromptToRemoveUsersFromGroup(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_PROMPT_REMOVE_USERS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message promptToRemoveUsersToGroup(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text("Выберете пользователей")
                .replyMarkup(promptRemoveUsersMarkup())
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuPromptToAddUsersToGroup(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_PROMPT_NEW_USERS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message promptToAddUsersToGroup(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text("Выберете пользователей")
                .replyMarkup(promptAddUsersMarkup())
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewGroupLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_PROMPT_NEW_LABEL)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewGroupBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_GROUP_PROMPT_NEW_BALANCE)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void reviewGroupUsers(UserState userState, Set<AppUser> users) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getUsersList(users))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void confirmDeactivateGroupMsg(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
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

    private ReplyKeyboardMarkup promptRemoveUsersMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(false)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text("Выбрать пользователей")
                                        .requestUsers(
                                                KeyboardButtonRequestUsers.builder()
                                                        .requestId("1")
                                                        .userIsBot(false)
                                                        .requestName(true)
                                                        .requestUsername(true)
                                                        .maxQuantity(ManageGroupNewGroupView.MAX_USERS_IN_GROUP)
                                                        .build())
                                        .build()))
                .build();
    }

    private ReplyKeyboardMarkup promptAddUsersMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(false)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text("Выбрать пользователей")
                                        .requestUsers(
                                                KeyboardButtonRequestUsers.builder()
                                                        .requestId("1")
                                                        .requestName(true)
                                                        .requestUsername(true)
                                                        .userIsBot(false)
                                                        .maxQuantity(ManageGroupNewGroupView.MAX_USERS_IN_GROUP)
                                                        .build())
                                        .build()))
                .build();
    }

    private String getBalanceDescription(Balance balance) {
        Tariff tariff = balance.getTariff();
        String tariffLabel = "";
        if (tariff == null) {
            logger.error("Tariff is null for balance: {}", balance.getId());
        } else {
            tariffLabel = String.format("%s (%s TRX, %s TRX)",
                    tariff.getLabel(),
                    FormattingTools.formatBalance(tariff.getTransactionType1AmountSun()),
                    FormattingTools.formatBalance(tariff.getTransactionType2AmountSun()));
        }

        return String.format("""
                ⚙️ Действия с группой

                Название: %s
                Менеджер: %s
                Создана: %s
                Тариф: %s
                Активна: %s

                Кошелек: `%s`
                Баланс: %s TRX
                """,
                balance.getLabel(),
                FormattingTools.formatUserLink(balance.getManager()),
                FormattingTools.formatDateToUtc(balance.getCreatedAt()),
                tariffLabel,
                balance.getIsActive() ? "✅" : "❌",
                balance.getDepositAddress(),
                FormattingTools.formatBalance(balance.getSunBalance()));
    }

    private InlineKeyboardMarkup getManageGroupActionsMarkup(Boolean showBackButton, Boolean canEdit) {
        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow(
                InlineKeyboardButton
                        .builder()
                        .text(StaticLabels.TO_MAIN_MENU)
                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                        .build());

        if (showBackButton) {
            inlineKeyboardRow.add(
                    InlineKeyboardButton
                            .builder()
                            .text(StaticLabels.GO_BACK)
                            .callbackData(InlineMenuCallbacks.GO_BACK)
                            .build());
        }

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup
                .builder();
        if (canEdit) {
                builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_SET_MANAGER)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_MANAGER)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_GROUP_ACTION_CHANGE_TARIFF)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_TARIFF)
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
                                        .build()));
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

    private InlineKeyboardMarkup getManageGroupActionsMarkupForManager() {
        return InlineKeyboardMarkup
                .builder()
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
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getUsersList(Set<AppUser> users) {
        String usersStr = users.isEmpty() ? "Пользователей нет"
                : users.stream()
                        .map(user -> String.format("[@%s](https://t.me/%s), %s", user.getTelegramUsername(), user.getTelegramUsername(), user.getTelegramFirstName()))
                        .collect(Collectors.joining("\n"));

        return """
                👥 Список пользователей группы

                %s
                """
                .formatted(usersStr);
    }
}
