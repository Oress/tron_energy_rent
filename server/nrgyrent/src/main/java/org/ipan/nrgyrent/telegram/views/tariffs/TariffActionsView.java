package org.ipan.nrgyrent.telegram.views.tariffs;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.TariffLabels;
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
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final TariffLabels tariffLabels;

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
                .text(tariffLabels.deactivateSuccess())
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
                .text(tariffLabels.renameSuccess())
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
                .text(tariffLabels.amountChangeSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void tariffNameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(tariffLabels.warnTariffLabelShort())
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
                .text(tariffLabels.promptNewLabel())
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
                .text(tariffLabels.warnDeactivate())
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

    private String getBalanceDescription(Tariff tariff) {
        return tariffLabels.preview(
                tariff.getLabel(),
                tariff.getPredefined() ? commonLabels.yes() : commonLabels.no(),
                FormattingTools.formatBalance(tariff.getTransactionType1AmountSun()),
                FormattingTools.formatBalance(tariff.getTransactionType2AmountSun()),
                FormattingTools.formatDateToUtc(tariff.getCreatedAt()),
                tariff.getActive() ? commonLabels.check() : commonLabels.cross(),
                tariff.getPredefined() ? commonLabels.defaultTariffWarning() : "");
    }

    private InlineKeyboardMarkup getManageTariffActionsMarkup(Boolean showBackButton, Boolean canChange) {
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

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        if (canChange) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(tariffLabels.menuRename())
                                    .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_RENAME)
                                    .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(tariffLabels.menuChangeTx1Amount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX1_AMOUNT)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(tariffLabels.menuChangeTx2Amount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX2_AMOUNT)
                                            .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(tariffLabels.menuDeactivate())
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_DEACTIVATE)
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
}
