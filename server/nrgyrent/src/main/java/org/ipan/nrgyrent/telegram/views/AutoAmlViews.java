package org.ipan.nrgyrent.telegram.views;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.autoaml.AutoAmlSession;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoAmlSession;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.AutoAmlLabels;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class AutoAmlViews {
    private final TelegramClient tgClient;
    private final CommonLabels commonLabels;
    private final AutoAmlLabels autoAmlLabels;

    @SneakyThrows
    public void showAutoAmlMenu(UserState userState, List<WalletWithAutoAmlSession> walletsWithSessions) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoAmlLabels.description(userState.getLocaleOrDefault()))
                .parseMode("MARKDOWN")
                .replyMarkup(getWalletsMenuMarkup(walletsWithSessions))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showAutoAmlMenu user: {}", userState, e);
        }
    }

    @SneakyThrows
    public Message showAutoAmlMenuMsg(UserState userState, List<WalletWithAutoAmlSession> walletsWithSessions) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(autoAmlLabels.description(userState.getLocaleOrDefault()))
                .parseMode("MARKDOWN")
                .replyMarkup(getWalletsMenuMarkup(walletsWithSessions))
                .build();
        return tgClient.execute(message);
    }

    public void showThresholdPrompt(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoAmlLabels.thresholdPrompt(userState.getLocaleOrDefault()))
                .replyMarkup(backButtonMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showThresholdPrompt user: {}", userState, e);
        }
    }

    public void showSessionCreated(UserState userState, AutoAmlSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(session.getChatId())
                .messageId(session.getMessageToUpdate())
                .text(autoAmlLabels.sessionStartMessage(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress()),
                        FormattingTools.formatUsdt(session.getThresholdSun())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showSessionCreated user: {}", userState, e);
        }
    }

    public void showSessionStopped(UserState userState, AutoAmlSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoAmlLabels.sessionStopMessage(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showSessionStopped user: {}", userState, e);
        }
    }

    public void showSessionStoppedLowBalance(UserState userState, AutoAmlSession session) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(autoAmlLabels.sessionStopLowBalance(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showSessionStoppedLowBalance user: {}", userState, e);
        }
    }

    public void showInvalidThreshold(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoAmlLabels.invalidThreshold(userState.getLocaleOrDefault()))
                .replyMarkup(backButtonMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to showInvalidThreshold user: {}", userState, e);
        }
    }

    private InlineKeyboardMarkup getWalletsMenuMarkup(List<WalletWithAutoAmlSession> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(walletSession -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(getWalletStatusText(walletSession))
                            .callbackData(InlineMenuCallbacks.createToggleAutoAmlCallback(walletSession.getWalletAddress(), walletSession.getActiveSessionId()))
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup.builder();
        walletRows.forEach(builder::keyboardRow);

        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.goBack())
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }

    private String getWalletStatusText(WalletWithAutoAmlSession walletSession) {
        String secondPart = WalletTools.formatTronAddressAndLabel(walletSession.getWalletAddress(), walletSession.getWalletLabel());
        String prefix = walletSession.getActiveSessionId() == null
                ? commonLabels.redCircle()
                : commonLabels.greenCircle();
        if (walletSession.getActiveSessionId() != null && walletSession.getThresholdUsdt() != null) {
            return prefix + " " + secondPart + " (>=" + FormattingTools.formatUsdt(walletSession.getThresholdUsdt()) + " USDT)";
        }
        return prefix + " " + secondPart;
    }

    private InlineKeyboardMarkup backButtonMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.goBack())
                                .callbackData(InlineMenuCallbacks.GO_BACK)
                                .build()))
                .build();
    }
}
