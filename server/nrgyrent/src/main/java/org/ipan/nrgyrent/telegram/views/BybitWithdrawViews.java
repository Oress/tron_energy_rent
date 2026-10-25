package org.ipan.nrgyrent.telegram.views;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.AdminLabels;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.BybitWithdrawParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@AllArgsConstructor
public class BybitWithdrawViews {
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final AdminLabels labels;

    public void promptAmount(UserState userState, String address, boolean invalid) {
        String text = labels.bybitWithdrawPromptAmount(address);
        if (invalid) {
            text = labels.bybitWithdrawInvalidAmount() + "\n\n" + text;
        }
        show(userState, text, commonViews.getToMainMenuAndBackMarkup());
    }

    public void confirm(UserState userState, BybitWithdrawParams params) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboardRow(new InlineKeyboardRow(
                InlineKeyboardButton.builder().text(commonLabels.no()).callbackData(InlineMenuCallbacks.CONFIRM_NO).build(),
                InlineKeyboardButton.builder().text(commonLabels.yes())
                        .callbackData(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId()).build())).build();
        show(userState, labels.bybitWithdrawConfirm(params.amount().toPlainString(), params.address()), markup);
    }

    public void noWallet(UserState userState) {
        show(userState, labels.bybitWithdrawNoWallet(), commonViews.getToMainMenuAndBackMarkup());
    }

    public void submitted(UserState userState, BybitWithdrawParams params, String withdrawalId) {
        show(userState, labels.bybitWithdrawSubmitted(params.amount().toPlainString(), params.address(), withdrawalId),
                commonViews.getToMainMenuAndBackMarkup());
    }

    public void rejected(UserState userState, Integer code, String reason) {
        String detail = reason == null ? "" : reason.substring(0, Math.min(reason.length(), 500));
        show(userState, labels.bybitWithdrawRejected(code.toString(), detail), commonViews.getToMainMenuAndBackMarkup());
    }

    public void unknown(UserState userState, String requestId) {
        show(userState, labels.bybitWithdrawUnknown(requestId), commonViews.getToMainMenuAndBackMarkup());
    }

    @SneakyThrows
    private void show(UserState userState, String text, InlineKeyboardMarkup markup) {
        tgClient.execute(EditMessageText.builder().chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId()).text(text).replyMarkup(markup).build());
    }
}
