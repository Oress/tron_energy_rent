package org.ipan.nrgyrent.telegram.views;

import java.util.List;

import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.StaticLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
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

@Component
@AllArgsConstructor
public class WalletsViews {
    public static final String OPEN_WALLET = "/wallet/";

    public static final String MSG_PROMPT_WALLET_ADDRESS = "Отправьте адрес кошелька TRC-20, который вы хотите добавить";
    public static final String MSG_PROMPT_WALLET_LABEL = "Отправьте название кошелька, который вы хотите добавить";
    public static final String MSG_ADD_WALLET_SUCCESS = "✅ Кошелек успешно добавлен";
    public static final String MSG_DELETE_WALLET_SUCCESS = "\uD83D\uDDD1\uFE0F Кошелек успешно удален";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @Retryable
    @SneakyThrows
    public void updMenuToWalletsMenu(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(StaticLabels.MSG_WALLETS)
                .replyMarkup(getWalletsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void showWalletDetails(UserWallet wallet, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getWalletDetails(wallet))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToDeleteWalletSuccessMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
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
    public void updMenuToPromptWalletAddress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_PROMPT_WALLET_ADDRESS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
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

    private String getWalletDetails(UserWallet wallet) {
        return String.format("Кошелек: %s\nАдрес: %s", wallet.getLabel(), wallet.getAddress());
    }

    private InlineKeyboardMarkup getWalletsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(getWalletLabel(wallet))
                            .callbackData(OPEN_WALLET + wallet.getId().toString())
                            .build(),
                    InlineKeyboardButton
                            .builder()
                            .text(StaticLabels.WLT_DELETE_WALLET)
                            .callbackData("delete_wallet " + wallet.getId().toString())
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(StaticLabels.WLT_ADD_WALLET)
                                        .callbackData(InlineMenuCallbacks.ADD_WALLETS)
                                        .build()));
        walletRows.forEach(builder::keyboardRow);

        return builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(StaticLabels.TO_MAIN_MENU)
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build())

        )
                .build();
    }

    private String getWalletLabel(UserWallet wallet) {
        return "%s (%s)".formatted(WalletTools.formatTronAddressSuffixOnly(wallet.getAddress()), wallet.getLabel());
    }
}
