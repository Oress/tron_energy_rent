package org.ipan.nrgyrent.telegram.views;

import java.math.BigDecimal;
import java.util.List;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.retry.annotation.Retryable;
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
public class TransactionsViews {
    private static final String MSG_NOT_ENOUGH_TRX = """
            ❌ Недостаточно средств на балансе
            Пожалуйста, пополните баланс и повторите попытку.
            """;

    private static final String MSG_TRANSACTION_PROGRESS = "Работаем, пожалуйста, подождите...";

    private static final String MSG_TRANSACTION_PENDING = """
            ⏳ Транзакция в процессе
            Пожалуйста, подождите до 5 минут. Бот отправит вам уведомление, когда транзакция завершится
            """;

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void updMenuToPromptTrxAmount(UserState userState, Tariff tariff) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(promptCustomTxAmount(tariff.getTransactionType1AmountSun()))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .parseMode("MARKDOWN")
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToCustomAmounTransaction65kMenu(List<UserWallet> wallets, UserState userState, Integer txAmount, Tariff tariff) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getCustomTransactions65kMenuLabel(txAmount, tariff.getTransactionType1AmountSun()))
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .parseMode("MARKDOWN")
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransaction65kMenu(List<UserWallet> wallets, UserState userState, Tariff tariff) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getTransaction65kMenuLabel(tariff.getTransactionType1AmountSun()))
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .parseMode("MARKDOWN")
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransaction131kMenu(List<UserWallet> wallets, UserState userState, Tariff tariff) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getTransaction131kMenuLabel(tariff.getTransactionType2AmountSun()))
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .parseMode("MARKDOWN")
                .build();
        tgClient.execute(message);
    }

    private String promptCustomTxAmount(Long sunAmountPerTx) {
        return """
                ⚡ Ручной ввод количества транзакций на кош с USDT (1 тр = %s TRX)⚡️

                👇 Введите *текстом* нужное число транзакций 👇
                """.formatted(FormattingTools.formatBalance(sunAmountPerTx));
    }

    private String getCustomTransactions65kMenuLabel(Integer txAmount, Long sunAmountPerTx) {
        BigDecimal trxTotalSun = new BigDecimal(sunAmountPerTx * txAmount);
        BigDecimal trxTotal = trxTotalSun.divide(AppConstants.trxToSunRate);;
        return """
                ⚡%s транзакций на кош с USDT (1 тр = %s TRX)⚡️
                Всего: *%s TRX*

                👇 Введите *текстом кошелек*, либо выберете из *списка* 👇
                """.formatted(txAmount, FormattingTools.formatBalance(sunAmountPerTx), trxTotal);
    }

    private String getTransaction65kMenuLabel(Long trxAmount) {
        return """
                ⚡ Транзакции (1 тр на кош с USDT, %s TRX)

                👇 Введите *текстом кошелек*, либо выберете из *списка* 👇
                """.formatted(FormattingTools.formatBalance(trxAmount));
    }

    private String getTransaction131kMenuLabel(Long trxAmount) {
        return """
                ⚡ Транзакции (1 тр на кош без USDT или биржу, %s TRX)

                👇 Введите *текстом кошелек*, либо выберете из *списка* 👇
                """.formatted(FormattingTools.formatBalance(trxAmount));
    }

    public void notEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_NOT_ENOUGH_TRX)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not notEnoughBalance userstate {}", userState, e);
        }
    }

    public void somethingWentWrong(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Что-то пошло не так. Пожалуйста, попробуйте позже.")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not somethingWentWrong userstate {}", userState, e);
        }
    }

    public void transactionToInactiveWallet(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Кошелек не активен. Пожалуйста, выберите другой кошелек.")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not transactionToInactiveWallet userstate {}", userState, e);
        }
    }

    public void itrxBalanceNotEnoughFunds(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("❌ Сервис временно недоступен. Пожалуйста, свяжитесь с администратором.")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not itrxBalanceNotEnoughFunds userstate {}", userState, e);
        }
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransactionInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_TRANSACTION_PROGRESS)
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToTransactionPending(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_TRANSACTION_PENDING)
                // .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getTransactionsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(WalletTools.formatTronAddressAndLabel(wallet.getAddress(), wallet.getLabel()))
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
                                        .build())

                )
                .build();
    }
}
