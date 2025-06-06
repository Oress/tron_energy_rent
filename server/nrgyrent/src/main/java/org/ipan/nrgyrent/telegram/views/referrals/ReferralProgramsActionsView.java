package org.ipan.nrgyrent.telegram.views.referrals;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.views.CommonViews;
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
public class ReferralProgramsActionsView {
    private static final String NO = "❌ Нет";
    private static final String YES = "✅ Да";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @SneakyThrows
    public void updMenuToManageRefProgramActionsMenu(UserState userState, ReferralProgram tariff) {
        boolean canChange = true;
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(tariff))
                .replyMarkup(getManageTariffActionsMarkup(true, canChange))
                .build();
        tgClient.execute(message);
    }

/*     @SneakyThrows
    public void deleteSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("✅ Реф. программа успешно деактивирована")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    } */

    @SneakyThrows
    public void renameSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("✅ Реф. программа успешно переименована")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void changeTxAmountSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("✅ Процент успешно изменен.")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void nameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Название слишком короткое. Минимум 3 символа. Попробуйте снова.")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not tariffNameIsTooShort userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void promptNewLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("Введите новое название реф. программы (минимум 3 символа):")
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void confirmDeactivateMsg(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("⚠️ Вы уверены, что хотите деактивировать тариф? (Все пользователи этого тарифа будут переведены на стандартный тариф.)")
                .replyMarkup(confirmDeleteTariffMarkup())
                .build();
        tgClient.execute(message);
    }

    public InlineKeyboardMarkup confirmDeleteTariffMarkup() {
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

    private String getBalanceDescription(ReferralProgram tariff) {
        return String.format("""
                ⚙️ Действия с реф. программой

                Название: %s
                Процент: %s

                Создана: %s
                """,
                tariff.getLabel(),
                tariff.getPercentage(),
                FormattingTools.formatDateToUtc(tariff.getCreatedAt())
                );
    }

    private InlineKeyboardMarkup getManageTariffActionsMarkup(Boolean showBackButton, Boolean canChange) {
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

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        if (canChange) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text("✏️ Переименовать")
                                    .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_RENAME)
                                    .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text("✏️ Изменить процент")
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_PERCENTAGE)
                                            .build()))
/*                 .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("❌ Деактивировать тариф")
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_DEACTIVATE)
                                        .build())) */
                                        ;
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
}
