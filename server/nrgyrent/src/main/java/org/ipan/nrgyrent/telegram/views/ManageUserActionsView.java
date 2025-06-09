package org.ipan.nrgyrent.telegram.views;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.ManageUserLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class ManageUserActionsView {
    public static final String OPEN_BALANCE = "/balance/";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final ManageUserLabels manageUserLabels;
    private final FormattingTools formattingTools;

    public void updMenuToManageUsersSearchResult(Page<AppUser> page, UserState userState) {
        String text = page.isEmpty() ? commonLabels.searchNoResults()
                : commonLabels.searchResults();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getUsersSearchPageMarkup(page))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updMenuToManageUsersSearchResult userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void updMenuToManageUserActionsMenu(UserState userState, AppUser appUser) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(appUser))
                .replyMarkup(getManageUserActionsMarkup(!appUser.getDisabled()))
                .build();
        tgClient.execute(message);
    }

    public void groupBalanceIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalanceNegative())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupBalanceIsNegative userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void userDeleted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.deactivateSuccess())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void userBalanceAdjusted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalanceSuccess())
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
                .text(manageUserLabels.changeTariffSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewUserBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalancePromptAmount())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void confirmDeactivateUserMsg(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.deactivateConfirm())
                .replyMarkup(confirmDeleteGroupMarkup(userState))
                .build();
        tgClient.execute(message);
    }

    public InlineKeyboardMarkup confirmDeleteGroupMarkup(UserState userState) {
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

    private String getBalanceDescription(AppUser user) {
        Tariff tariff = user.getBalance().getTariff();
        String tariffLabel = "";
        if (tariff == null) {
            logger.error("Tariff is null for user: {}", user.getTelegramId());
        } else {
            tariffLabel = String.format("%s (%s TRX, %s TRX)", 
            tariff.getLabel(), 
            FormattingTools.formatBalance(tariff.getTransactionType1AmountSun()),
            FormattingTools.formatBalance(tariff.getTransactionType2AmountSun()));
        }

        // TODO: view group if present
        return  manageUserLabels.preview( 
                user.getTelegramId().toString(),
                FormattingTools.valOrDash(user.getTelegramUsername()),
                FormattingTools.valOrDash(user.getTelegramFirstName()),
                tariffLabel,
                user.getDisabled() ? commonLabels.cross() : commonLabels.check(),
                user.getBalance().getDepositAddress(),
                FormattingTools.formatBalance(user.getBalance().getSunBalance())
                );
    }

    private InlineKeyboardMarkup getManageUserActionsMarkup(Boolean showDeactivateBtn) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeTariff())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_TARIFF)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeBalance())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .build()));

        if (showDeactivateBtn) {
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuDeactivate())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_DEACTIVATE)
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

    private InlineKeyboardMarkup getUsersSearchPageMarkup(Page<AppUser> page) {
        List<InlineKeyboardRow> users = page.getContent().stream().map(user -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(formattingTools.formatUserForSearch(user))
                            .callbackData(openBalanceRequest(user.getTelegramId()))
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        users.forEach(builder::keyboardRow);

        builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.searchReset())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS_SEARCH_RESET)
                                        .build()));

        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchPrevPage())
                                .callbackData(InlineMenuCallbacks.MANAGE_USERS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchNextPage())
                                .callbackData(InlineMenuCallbacks.MANAGE_USERS_NEXT_PAGE)
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

    private String openBalanceRequest(Long userId) {
        return OPEN_BALANCE + userId;
    }
}
