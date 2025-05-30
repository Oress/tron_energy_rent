package org.ipan.nrgyrent.telegram.views.tariffs;

import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.views.CommonViews;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class TariffNewView {
    private static final String MSG_MANAGE_TARIFF_ADD_SUCCESS = "✅ Тариф успешно добавлен";

    private static final String MSG_MANAGE_TARIFF_ADD_PROMPT_LABEL = "Введите название тарифа (минимум 3 символа):";
    private static final String MSG_MANAGE_TARIFF_ADD_PROMPT_TX1_AMOUNT = "Введите сумму TRX за 65 000 энергии:";
    private static final String MSG_MANAGE_TARIFF_ADD_PROMPT_TX2_AMOUNT = "Введите сумму TRX за 131 000 энергии:";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;

    @SneakyThrows
    public void updMenuToManageGroupsAddPromptLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_TARIFF_ADD_PROMPT_LABEL)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptTxType1Amount(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_TARIFF_ADD_PROMPT_TX1_AMOUNT)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptTxType2Amount(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_TARIFF_ADD_PROMPT_TX2_AMOUNT)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void tariffAddSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(MSG_MANAGE_TARIFF_ADD_SUCCESS)
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }
}
