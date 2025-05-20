package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
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
    private static final String MSG_WITHDRAW_TRX = """
            💰 Вывод TRX

            Выберите кошелек, на который хотите вывести TRX или введите адрес кошелька, на который хотите вывести средства.
            """;

    private static final String MSG_WITHDRAW_TRX_IN_PROGRESS = """
            💰 Вывод TRX

            Вывод средств в процессе. Вам будет отправлено уведомление, когда средства будут выведены.
            """;

    private static final String MENU_ADMIN_MANAGE_GROUPS = "👥 Управление группами";
    private static final String MENU_ADMIN_MANAGE_USERS = "👤 Управление пользователями";
    private static final String MENU_ADMIN_ITRX_BALANCE = "💰 Статистика itrx.io";
    private static final String MENU_ADMIN_SWEEP_WALLETS_BALANCE = "💰 Статистика sweep кошельков";
    private static final String MENU_ADMIN_WITHDRAW_TRX = "💰 Вывод TRX";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void withdrawTrxInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_WITHDRAW_TRX_IN_PROGRESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void withdrawTrx(List<UserWallet> wallets, CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_WITHDRAW_TRX)
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void itrxBalance(CallbackQuery callbackQuery, ApiUsageResponse apiUsageResponse) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(getItrxBalanceMessage(apiUsageResponse))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
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
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
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
                .keyboardRow( // TODO: show this button only for admin
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(MENU_ADMIN_WITHDRAW_TRX)
                                        .callbackData(InlineMenuCallbacks.MANAGE_WITHDRAW_TRX)
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

    private InlineKeyboardMarkup getTransactionsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(wallet.getLabel())
                            .callbackData(wallet.getAddress())
                            .build());
            return row;
        }).toList();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        walletRows.forEach(builder::keyboardRow);

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
