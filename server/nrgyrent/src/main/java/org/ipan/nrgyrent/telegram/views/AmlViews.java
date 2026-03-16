package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.Tariff;
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

    @SneakyThrows
    public void showAmlMenu(UserState userState, Tariff tariff) {
        String price = tariff != null && tariff.getAmlCheckPriceSun() != null
                ? FormattingTools.formatBalance(tariff.getAmlCheckPriceSun())
                : "N/A";

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.amlMenuDescription(userState.getLocaleOrDefault(), price))
                .parseMode("MARKDOWN")
                .replyMarkup(amlMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlMenu user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void showAmlPromptWallet(UserState userState, Balance balance, Tariff tariff) {
        String price = tariff != null && tariff.getAmlCheckPriceSun() != null
                ? FormattingTools.formatBalance(tariff.getAmlCheckPriceSun())
                : "N/A";
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
    public void showAmlInsufficientBalance(UserState userState, Balance balance, Tariff tariff) {
        String price = tariff != null && tariff.getAmlCheckPriceSun() != null
                ? FormattingTools.formatBalance(tariff.getAmlCheckPriceSun())
                : "N/A";
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
    public void showAmlHistory(UserState userState, List<AmlVerification> history) {
        StringBuilder sb = new StringBuilder();
        if (history.isEmpty()) {
            sb.append(commonLabels.amlHistoryEmpty(userState.getLocaleOrDefault()));
        } else {
            for (AmlVerification v : history) {
                sb.append(commonLabels.amlHistoryItem(userState.getLocaleOrDefault(), v)).append("\n\n");
            }
        }

        EditMessageText message = EditMessageText.builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(sb.toString().trim())
                .parseMode("MARKDOWN")
                .replyMarkup(amlHistoryMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAmlHistory user: {}", userState, e);
        }
    }

    private InlineKeyboardMarkup amlMenuMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.amlMenuCheckWallet())
                                .callbackData(InlineMenuCallbacks.AML_CHECK)
                                .build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.amlMenuHistory())
                                .callbackData(InlineMenuCallbacks.AML_HISTORY)
                                .build()))
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.toMainMenu())
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build()))
                .build();
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
