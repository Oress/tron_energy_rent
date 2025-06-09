package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.ManageGroupsLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
public class ManageGroupNewGroupView {
    public static final Integer MAX_USERS_IN_GROUP = 10;

    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final ManageGroupsLabels manageGroupsLabels;

    public void userIsManagerInAnotherGroup(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGroupsLabels.userManagesOtherGroup())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not userIsManagerInAnotherGroup userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void updMenuToManageGroupsMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGroupsLabels.msg())
                .replyMarkup(getManageGroupsMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updMenuToManageGroupsAddPromptLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGroupsLabels.createPromptLabel())
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
                .text(manageGroupsLabels.createPromptManager())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendAddPromptManager(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(manageGroupsLabels.assignManagerPromptChooseUser())
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
                .text(manageGroupsLabels.createSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }


    private ReplyKeyboardMarkup getManageGroupsNewGroupPromptManagerMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(true)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text(manageGroupsLabels.assignManagerPromptChooseManager())
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

    private InlineKeyboardMarkup getManageGroupsMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.manageSearch())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_SEARCH)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.manageAdd())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ADD)
                                        .build()))
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
