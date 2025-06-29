package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.ManageGroupsLabels;
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
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final ManageGroupsLabels manageGrouopsLabels;
    private final FormattingTools formattingTools;

    public void somethingWentWrong(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.somethingWentWrong())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not somethingWentWrong userstate {}", userState, e);
        }
    }

    public void cannotRemoveManager(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.removeUsersCantRemoveMngr())
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
                .text(manageGrouopsLabels.usersNotRegistered(list))
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
                .text(manageGrouopsLabels.usersBelongToAnotherGroup())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not someUsersAreNotRegistered userstate {}", userState, e);
        }
    }

    public void userDisabled(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.usersDisabled())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not userDisabled userstate {}", userState, e);
        }
    }

    public void userNotManager(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.notManager())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not userDisabled userstate {}", userState, e);
        }
    }

    public void groupBalanceIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.changeBalanceNotNegative())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupBalanceIsNegative userstate {}", userState, e);
        }
    }

    public void groupWithdrawLimitIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.changeWithdrawLimitNotNegative())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupWithdrawLimitIsNegative userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void updMenuManagerChanged(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.assignManagerSuccess())
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
                .text(manageGrouopsLabels.assignManagerPrompt())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message sendPromptManager(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(manageGrouopsLabels.assignManagerPromptChooseUser())
                .replyMarkup(getManageGroupsNewGroupPromptManagerMarkup())
                .build();
        return tgClient.execute(message);
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
                                        .text(manageGrouopsLabels.assignManagerPromptChooseManager())
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
                .text(manageGrouopsLabels.deactivateSuccess())
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
                .text(manageGrouopsLabels.renameSuccess())
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
                .text(manageGrouopsLabels.changeTariffSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void groupNameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.renameLabelShort())
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
                .text(manageGrouopsLabels.changeBalanceSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void groupWithdrawLimitAdjusted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.changeWithdrawLimitSuccess())
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
                .text(manageGrouopsLabels.addUsersSuccess())
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
                .text(manageGrouopsLabels.removeUsersSuccess())
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
                .text(manageGrouopsLabels.removeUsersPromptUsers())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message promptToRemoveUsersToGroup(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(manageGrouopsLabels.chooseUsers())
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
                .text(manageGrouopsLabels.addUsersPromptUsers())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message promptToAddUsersToGroup(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(manageGrouopsLabels.chooseUsers())
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
                .text(manageGrouopsLabels.renamePromptLabel())
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
                .text(manageGrouopsLabels.changeBalancePromptBalance())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewWithdrawLimit(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageGrouopsLabels.changeWithdrawLimitPromptTotal())
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
                .text(manageGrouopsLabels.deactivateConfirm())
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
                                        .text(commonLabels.no())
                                        .callbackData(InlineMenuCallbacks.CONFIRM_NO)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.yes())
                                        .callbackData(InlineMenuCallbacks.CONFIRM_YES)
                                        .build()))
                .build();
    }

    private ReplyKeyboardMarkup promptRemoveUsersMarkup() {
        return ReplyKeyboardMarkup
                .builder()
                .isPersistent(true)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text(manageGrouopsLabels.chooseUsers())
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
                .isPersistent(true)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .keyboardRow(
                        new KeyboardRow(
                                KeyboardButton.builder()
                                        .text(manageGrouopsLabels.chooseUsers())
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

        return manageGrouopsLabels.preview(
                balance.getLabel(),
                formattingTools.formatUserForSearch(balance.getManager()),
                FormattingTools.formatDateToUtc(balance.getCreatedAt()),
                tariffLabel,
                balance.getIsActive() ? commonLabels.check() : commonLabels.cross(),
                balance.getDepositAddress(),
                FormattingTools.formatBalance(balance.getSunBalance()),
                FormattingTools.formatBalance(balance.getDailyWithdrawalLimitSun()),
                FormattingTools.formatBalance(balance.getDailyWithdrawalRemainingSun())
                );
    }

    private InlineKeyboardMarkup getManageGroupActionsMarkup(Boolean showBackButton, Boolean canEdit) {
        InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow(
                InlineKeyboardButton
                        .builder()
                        .text(commonLabels.toMainMenu())
                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                        .build());

        if (showBackButton) {
            inlineKeyboardRow.add(
                    InlineKeyboardButton
                            .builder()
                            .text(commonLabels.goBack())
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
                                        .text(manageGrouopsLabels.menuAssignManager())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_MANAGER)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuChangeBalance())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuChangeWithdrawLimit())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADJUST_WITHDRAW_LIMIT)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuChangeTariff())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_TARIFF)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuRename())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_RENAME)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuReviewUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuAddUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuRemoveUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuDeactivate())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DEACTIVATE)
                                        .build()));
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

    private InlineKeyboardMarkup getManageGroupActionsMarkupForManager() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuReviewUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuAddUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageGrouopsLabels.menuRemoveUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getUsersList(Set<AppUser> users) {
        String usersStr = users.isEmpty() ? manageGrouopsLabels.usersListEmpty()
                : users.stream()
                        .map(user -> formattingTools.formatUserForSearch(user))
                        .collect(Collectors.joining("\n"));

        return manageGrouopsLabels.usersList(usersStr);
    }
}
