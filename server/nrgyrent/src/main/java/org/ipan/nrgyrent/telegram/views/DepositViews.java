package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class DepositViews {
    private final CommonViews commonViews;
    private final TelegramClient tgClient;

    @Retryable
    @SneakyThrows
    public void updMenuToDepositsMenu(UserState userState, AppUser user) {
        Balance personalBalance = user.getBalance();
        Balance groupBalance = user.getGroupBalance();

        String text = groupBalance != null
            ? getGroupDepositMenuText(groupBalance.getDepositAddress(), groupBalance.getSunBalance())
            : getPersonalDepositMenuText(personalBalance.getDepositAddress(), personalBalance.getSunBalance());

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .parseMode("MARKDOWN")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    public static String getGroupDepositMenuText(String groupDepositAddress, Long groupSunBalance) {
        return """
                👛 Адрес депозита группы:

                `%s`

                *Баланс группы: %s TRX*
 
                =========================

                ❗️Вы можете отправить TRX только в сети TRC-20❗️

                ❗️ Минимальный депозит - 10 TRX❗️

                ⌛️ Среднее время зачисления депозита - 2 минуты, вы получите уведомление при успешном пополнении"""
                .formatted(groupDepositAddress,FormattingTools.formatBalance(groupSunBalance));
    }

    public static String getPersonalDepositMenuText(String depositAddress, Long sunBalance) {
        return """
                💰 Ваш адрес депозита:

                `%s`

                *Ваш баланс: %s TRX*

                ❗️Вы можете отправить TRX только в сети TRC-20❗️

                ❗️ Минимальный депозит - 10 TRX❗️

                ⌛️ Среднее время зачисления депозита - 2 минуты, вы получите уведомление при успешном пополнении"""
                .formatted(depositAddress, FormattingTools.formatBalance(sunBalance));
    }
}
