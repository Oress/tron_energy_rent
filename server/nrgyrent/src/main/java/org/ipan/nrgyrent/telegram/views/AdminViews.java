package org.ipan.nrgyrent.telegram.views;

import java.util.Map;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class AdminViews {

    private static final String MSG_ADMIN_MENU = """
            👨‍💻 Админка

            Здесь вы можете управлять группами, пользователями, а также просматривать и изменять их баланс
            """;

    private static final String MENU_ADMIN_MANAGE_GROUPS = "👥 Управление группами";
    private static final String MENU_ADMIN_MANAGE_USERS = "👤 Управление пользователями";
    private static final String MENU_ADMIN_ITRX_BALANCE = "💰 Статистика itrx.io";
    private static final String MENU_ADMIN_SWEEP_WALLETS_BALANCE = "💰 Статистика sweep кошельков";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void itrxBalance(CallbackQuery callbackQuery, ApiUsageResponse apiUsageResponse) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getItrxBalanceMessage(apiUsageResponse))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void sweepWalletsBalance(CallbackQuery callbackQuery, Map<CollectionWallet, Long> results) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getSweepBalanceMessage(results))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAdminMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_ADMIN_MENU)
                .replyMarkup(getAdminMenuReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getAdminMenuReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MENU_ADMIN_MANAGE_GROUPS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MENU_ADMIN_MANAGE_USERS)
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MENU_ADMIN_ITRX_BALANCE)
                                        .callbackData(InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MENU_ADMIN_SWEEP_WALLETS_BALANCE)
                                        .callbackData(InlineMenuCallbacks.MANAGE_SWEEP_BALANCE)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.TO_MAIN_MENU)
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getItrxBalanceMessage(ApiUsageResponse apiUsageResponse) {
        return """
                💰 Статистика itrx.io

                Баланс: %s TRX
                Всего выполненых заказов: %s
                Всего энергии делегировано: %s
                Комиссия сервиса за все время: %s TRX
                """.formatted(
                FormattingTools.formatBalance(apiUsageResponse.getBalance()),
                FormattingTools.formatNumber(apiUsageResponse.getTotal_count()),
                FormattingTools.formatNumber(apiUsageResponse.getTotal_sum_energy()),
                FormattingTools.formatBalance(apiUsageResponse.getTotal_sum_trx()));
    }

    private String getSweepBalanceMessage(Map<CollectionWallet, Long> results) {
        return """
                💰 Статистика sweep кошельков

                %s
                """.formatted(
                results.entrySet().stream()
                        .map(kv -> String.format("Адрес: %s\n Баланс: %s TRX", kv.getKey().getWalletAddress(),
                                FormattingTools.formatBalance(kv.getValue())))
                        .collect(Collectors.joining("\n\n")));
    }
}
