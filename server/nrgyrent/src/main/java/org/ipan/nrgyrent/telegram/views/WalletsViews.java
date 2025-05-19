package org.ipan.nrgyrent.telegram.views;

import org.h2.engine.User;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class WalletsViews {
    public static final String MSG_PROMPT_WALLET_ADDRESS = "Отправьте адрес кошелька TRC-20, который вы хотите добавить";
    public static final String MSG_PROMPT_WALLET_LABEL = "Отправьте название кошелька, который вы хотите добавить";
    public static final String MSG_ADD_WALLET_SUCCESS = "✅ Кошелек успешно добавлен";
    public static final String MSG_DELETE_WALLET_SUCCESS = "\uD83D\uDDD1\uFE0F Кошелек успешно удален";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void updMenuToDeleteWalletSuccessMenu(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_DELETE_WALLET_SUCCESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAddWalletSuccessMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_ADD_WALLET_SUCCESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToPromptWalletAddress(CallbackQuery callbackQuery) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(MSG_PROMPT_WALLET_ADDRESS)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToPromptWalletLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_PROMPT_WALLET_LABEL)
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }
}
