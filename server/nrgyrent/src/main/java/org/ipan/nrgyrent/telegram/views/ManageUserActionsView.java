package org.ipan.nrgyrent.telegram.views;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
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
    private static final String NEXT_PAGE = "➡️";
    private static final String PREV_PAGE = "⬅️";
    private static final String MANAGE_USER_ACTION_CHANGE_TARIFF = "🔄 Изменить тариф";
    private static final String MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY = "💰 Изменить баланс вручную";
    private static final String MANAGE_USER_ACTION_DEACTIVATE = "❌ Деактивировать пользователя";

    private static final String MSG_DEACTIVATE_USER_WARNING = "⚠️ Вы уверены, что хотите деактивировать пользователя?";
    private static final String MSG_USER_DEACTIVATED = "✅ Пользователь успешно деактивирован.";
    private static final String MSG_USER_PROMPT_NEW_BALANCE = "Введите новый баланс пользователя (в TRX)";
    private static final String MSG_USER_BALANCE_ADJUSTED = "✅ Баланс пользователя успешно изменен.";
    private static final String MSG_USER_TARIFF_CHANGED = "✅ Тариф пользователя успешно изменен.";

    private static final String MANAGE_USERS_SEARCH_RESET = "🔄 Сбросить поиск";

    public static final String OPEN_BALANCE = "/balance/";
    private static final String MSG_MANAGE_USERS_SEARCH_NO_RESULTS = "❌ Нет результатов";
    private static final String MSG_MANAGE_USERS_SEARCH_PAGE_RESULTS = """
            🔍 Результаты поиска
            Используйте стрелки, чтобы прокручивать результаты, или введите имя пользователя, чтобы найти его.

            Выберите пользователя, с которой хотите работать
            """;

    private static final String NO = "❌ Нет";
    private static final String YES = "✅ Да";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    public void updMenuToManageUsersSearchResult(Page<AppUser> page, UserState userState) {
        String text = page.isEmpty() ? MSG_MANAGE_USERS_SEARCH_NO_RESULTS
                : MSG_MANAGE_USERS_SEARCH_PAGE_RESULTS;

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
                .text("❌ Баланс не может быть отрицательным. Попробуйте снова.")
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
                .text(MSG_USER_DEACTIVATED)
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
                .text(MSG_USER_BALANCE_ADJUSTED)
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

    @SneakyThrows
    public void promptNewUserBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_USER_PROMPT_NEW_BALANCE)
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
                .text(MSG_DEACTIVATE_USER_WARNING)
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
        return String.format("""
                ⚙️ Действия с пользователем

                ID: %s
                Логин: %s
                Имя пользователя: %s
                Тариф: %s
                Активен: %s

                Кошелек: %s
                Баланс: %s TRX
                """, 
                user.getTelegramId(),
                user.getTelegramUsername(),
                user.getTelegramFirstName(),
                tariffLabel,
                user.getDisabled() ? "❌" : "✅",
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
                                        .text(MANAGE_USER_ACTION_CHANGE_TARIFF)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_TARIFF)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .build()));

        if (showDeactivateBtn) {
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_USER_ACTION_DEACTIVATE)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_DEACTIVATE)
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

    private InlineKeyboardMarkup getUsersSearchPageMarkup(Page<AppUser> page) {
        List<InlineKeyboardRow> users = page.getContent().stream().map(user -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(user.getTelegramUsername())
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
                                        .text(MANAGE_USERS_SEARCH_RESET)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS_SEARCH_RESET)
                                        .build()));

        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(PREV_PAGE)
                                .callbackData(InlineMenuCallbacks.MANAGE_USERS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(NEXT_PAGE)
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

    private String openBalanceRequest(Long userId) {
        return OPEN_BALANCE + userId;
    }
}
