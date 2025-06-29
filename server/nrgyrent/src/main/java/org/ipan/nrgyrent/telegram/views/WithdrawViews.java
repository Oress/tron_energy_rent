package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.WithdrawLabels;
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
@Slf4j
@AllArgsConstructor
public class WithdrawViews {
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final WithdrawLabels withdrawLabels;

    @Retryable
    @SneakyThrows
    public void withdrawTrxInactiveWallet(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.transactionToInactiveWallet())
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void withdrawTrxInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.inProgress())
                // .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updWithdrawalSuccessful(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .text(withdrawLabels.success())
                .messageId(userState.getMenuMessageId())
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updWithdrawalFailNotEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.notEnoughtBalance())
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updWithdrawalFailNotEnoughLimit(UserState userState, Long dailyWithdrawalRemainingSun) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.notEnoughLimit(FormattingTools.formatBalance(dailyWithdrawalRemainingSun)))
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updWithdrawalFailServiceNotEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.serviceNotEnoughtBalance())
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    public void updWithdrawalFail(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.fail())
                .replyMarkup(getOrderRefundedNotificationMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch(Exception e) {
            logger.error("Could not updWithdrawalFail", e);
        }
    }

    @SneakyThrows
    public void updNotEnoughRights(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .text(withdrawLabels.notEnoughRights())
                .messageId(userState.getMenuMessageId())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    public void promptAmountAgainNotEnoughBalance(UserState userState, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getPromptAmountForWithdrawalNotEnoughBalance(balance))
                .parseMode("MARKDOWN")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not promptAmountAgainNotEnoughBalance", e);
        }
    }

    public void promptAmountAgainNotEnoughLimit(UserState userState, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getPromptAmountForWithdrawalNotEnoughLimit(balance))
                .parseMode("MARKDOWN")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not promptAmountAgainNotEnoughBalance", e);
        }
    }

    @Retryable
    @SneakyThrows
    public void promptAmount(UserState userState, Balance balance) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getPromptAmountForWithdrawal(balance))
                .parseMode("MARKDOWN")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptWallets(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(withdrawLabels.promptWallet())
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getOrderRefundedNotificationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build())

                )
                .build();
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
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getPromptAmountForWithdrawal(Balance balance) {
        long max = Long.max(0, Long.min(balance.getDailyWithdrawalRemainingSun(), balance.getSunBalance() - AppConstants.WITHDRAWAL_FEE));
        return withdrawLabels.promptAllowedToWithdraw(FormattingTools.formatBalance(max));
    }

    private String getPromptAmountForWithdrawalNotEnoughBalance(Balance balance) {
        return withdrawLabels.promptNotEnoughtBalance(FormattingTools.formatBalance(Long.max(0, balance.getSunBalance() - AppConstants.WITHDRAWAL_FEE)));
    }

    private String getPromptAmountForWithdrawalNotEnoughLimit(Balance balance) {
        long max = Long.max(0, Long.min(balance.getDailyWithdrawalRemainingSun(), balance.getSunBalance() - AppConstants.WITHDRAWAL_FEE));
        return withdrawLabels.promptNotEnoughLimit(FormattingTools.formatBalance(max));
    }
}
