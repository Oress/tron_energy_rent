package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
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
@AllArgsConstructor
@Slf4j
public class AmlViews {
    private final TelegramClient tgClient;
    private final CommonLabels commonLabels;
    private final FormattingTools formattingTools;

    @SneakyThrows
    public void showAmlMenu(UserState userState, String estimatedPriceTrx, AmlProvider provider) {
        String price = estimatedPriceTrx != null ? estimatedPriceTrx : "N/A";

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.amlMenuDescription(userState.getLocaleOrDefault(), price))
                .parseMode("MARKDOWN")
                .replyMarkup(amlMenuMarkup(provider, UserRole.ADMIN.equals(userState.getRole())))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlMenu user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlPromptWallet(UserState userState, Balance balance, String estimatedPriceTrx) {
        String price = estimatedPriceTrx != null ? estimatedPriceTrx : "N/A";
        String currentBalance = balance != null
                ? FormattingTools.formatBalance(balance.getSunBalance())
                : "N/A";

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.amlPromptWallet(userState.getLocaleOrDefault(), price, currentBalance))
                .parseMode("MARKDOWN")
                .replyMarkup(amlHistoryMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlPromptWallet user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlInsufficientBalance(UserState userState, Balance balance, String estimatedPriceTrx) {
        String price = estimatedPriceTrx != null ? estimatedPriceTrx : "N/A";
        String currentBalance = balance != null
                ? FormattingTools.formatBalance(balance.getSunBalance())
                : "N/A";

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.amlInsufficientBalance(userState.getLocaleOrDefault(), price, currentBalance))
                .parseMode("MARKDOWN")
                .replyMarkup(amlHistoryMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlInsufficientBalance user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlRequestReceived(UserState userState, String walletAddress) {
        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.amlRequestReceived(userState.getLocaleOrDefault(), walletAddress))
                .parseMode("MARKDOWN")
//                .replyMarkup(amlHistoryMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlRequestReceived user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlHistory(UserState userState, List<AmlVerification> history) {
        String text;
        InlineKeyboardMarkup markup;
        if (history.isEmpty()) {
            text = commonLabels.amlHistoryEmpty(userState.getLocaleOrDefault());
            markup = amlHistoryMarkup();
        } else {
            text = commonLabels.amlHistoryHeader(userState.getLocaleOrDefault());
            markup = amlHistoryItemsMarkup(history, userState.getLocaleOrDefault());
        }

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .parseMode("MARKDOWN")
                .replyMarkup(markup)
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlHistory user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlVerificationReport(UserState userState, AmlVerification v) {
        String text;
        if (AmlVerificationStatus.COMPLETED.equals(v.getStatus())) {
            text = formattingTools.formatAmlReport(v, userState.getLocaleOrDefault());
        } else {
            text = commonLabels.amlReportFailed(userState.getLocaleOrDefault(), v.getWalletAddress());
        }

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .parseMode("MARKDOWN")
                .replyMarkup(amlHistoryMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlVerificationReport user: {}", userState, e);
        }
    }

    private InlineKeyboardMarkup amlMenuMarkup(AmlProvider provider, boolean isAdmin) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup.builder();
        builder
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.amlMenuCheckWallet())
                                .callbackData(InlineMenuCallbacks.AML_CHECK)
                                .build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.amlMenuHistory())
                                .callbackData(InlineMenuCallbacks.AML_HISTORY)
                                .build()));

        if (isAdmin) {
            builder
                    .keyboardRow(new InlineKeyboardRow(
                            InlineKeyboardButton.builder()
                                    .text(commonLabels.autoAmlButton())
                                    .callbackData(InlineMenuCallbacks.AUTO_AML)
                                    .build()));
        }
        return builder.keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.settingsAmlProvider(provider))
                                .callbackData(InlineMenuCallbacks.SETTINGS_AML_PROVIDER)
                                .build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.toMainMenu())
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build()))
                .build();
    }

    private InlineKeyboardMarkup amlHistoryItemsMarkup(List<AmlVerification> history, java.util.Locale locale) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        for (AmlVerification v : history) {
            builder.keyboardRow(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(formattingTools.formatAmlHistoryItemLabel(v, locale))
                            .callbackData(InlineMenuCallbacks.getAmlViewItemCallback(v.getId()))
                            .build()));
        }
        builder.keyboardRow(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text(commonLabels.toMainMenu())
                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                        .build(),
                InlineKeyboardButton.builder()
                        .text(commonLabels.goBack())
                        .callbackData(InlineMenuCallbacks.GO_BACK)
                        .build()));
        return builder.build();
    }

    private InlineKeyboardMarkup amlHistoryMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.toMainMenu())
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(commonLabels.goBack())
                                .callbackData(InlineMenuCallbacks.GO_BACK)
                                .build()))
                .build();
    }
}
