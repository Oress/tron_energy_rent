package org.ipan.nrgyrent.telegram.views.tariffs;

import org.ipan.nrgyrent.domain.model.Tariff;
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
public class TariffActionsView {
    private static final String MANAGE_TARIFF_ACTION_CHANGE_TX1_AMOUNT = "✏️ Изменить сумму за 65к";
    private static final String MANAGE_TARIFF_ACTION_CHANGE_TX2_AMOUNT = "✏️ Изменить сумму за 131к";
    private static final String MANAGE_TARIFF_ACTION_RENAME = "✏️ Переименовать тариф";
    private static final String MANAGE_TARIFF_ACTION_DEACTIVATE = "❌ Деактивировать тариф";

    private static final String MSG_DELETE_TARIFF_WARNING = "⚠️ Вы уверены, что хотите деактивировать тариф? (Все пользователи этого тарифа будут переведены на стандартный тариф.)";
    private static final String MSG_TARIFF_DELETED = "✅ Тариф успешно деактивирован.";
    private static final String MSG_TARIFF_PROMPT_NEW_LABEL = "Введите новое название тарифа (минимум 3 символа):";
    private static final String MSG_TARIFF_RENAMED = "✅ Тариф успешно переименован.";
    private static final String MSG_TARIFF_AMOUNT_CHANGED = "✅ Сумма транзакции успешно изменена.";
    private static final String MSG_TARIFF_TOO_SHORT = "❌ Название слишком короткое. Минимум 3 символа. Попробуйте снова.";

    private static final String NO = "❌ Нет";
    private static final String YES = "✅ Да";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @SneakyThrows
    public void updMenuToManageTariffActionsMenu(UserState userState, Tariff tariff) {
        boolean canChange = !tariff.getPredefined() && tariff.getActive();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(tariff))
                .replyMarkup(getManageTariffActionsMarkup(true, canChange))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void deleteSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_TARIFF_DELETED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void renameSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_TARIFF_RENAMED)
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
                .text(MSG_TARIFF_AMOUNT_CHANGED)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void tariffNameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_TARIFF_TOO_SHORT)
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
                .text(MSG_TARIFF_PROMPT_NEW_LABEL)
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
                .text(MSG_DELETE_TARIFF_WARNING)
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

    private String getBalanceDescription(Tariff tariff) {
        return String.format("""
                ⚙️ Действия с тарифом

                Название: %s
                Создан системой: %s
                65к енергии: %s TRX
                131к енергии: %s TRX

                Создан: %s
                Активен: %s
                %s
                """,
                tariff.getLabel(),
                tariff.getPredefined() ? "Да" : "Нет",
                FormattingTools.formatBalance(tariff.getTransactionType1AmountSun()),
                FormattingTools.formatBalance(tariff.getTransactionType2AmountSun()),
                FormattingTools.formatDateToUtc(tariff.getCreatedAt()),
                tariff.getActive() ? "✅" : "❌",
                tariff.getPredefined() ? "\nP.S. Стандартный тариф не может быть изменен." : "");
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
                                    .text(MANAGE_TARIFF_ACTION_RENAME)
                                    .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_RENAME)
                                    .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(MANAGE_TARIFF_ACTION_CHANGE_TX1_AMOUNT)
                                            .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX1_AMOUNT)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(MANAGE_TARIFF_ACTION_CHANGE_TX2_AMOUNT)
                                            .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX2_AMOUNT)
                                            .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MANAGE_TARIFF_ACTION_DEACTIVATE)
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_DEACTIVATE)
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
}
