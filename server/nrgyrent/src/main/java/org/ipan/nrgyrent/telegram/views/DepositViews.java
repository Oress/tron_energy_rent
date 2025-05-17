package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
    public void updMenuToDepositsMenu(CallbackQuery callbackQuery, AppUser user) {
        Balance personalBalance = user.getBalance();
        Balance groupBalance = user.getGroupBalance();

        String text = groupBalance != null
            ? getDepositMenuText(personalBalance.getDepositAddress(), personalBalance.getSunBalance(), groupBalance.getDepositAddress(), groupBalance.getSunBalance())
            : getDepositMenuText(personalBalance.getDepositAddress(), personalBalance.getSunBalance());

        EditMessageText message = EditMessageText
                .builder()
                .chatId(callbackQuery.getMessage().getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .text(text)
                .parseMode("MARKDOWN")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    public static String getDepositMenuText(String personalDepositAddress, Long personalSunBalance, String groupDepositAddress, Long groupSunBalance) {
        return """
                💰 Ваш адресс депозита:

                `%s`

                💰 Баланс:

                *%s* TRX

                =========================

                💰 Адресс депозита группы:

                `%s`

                💰 Баланс группы:

                *%s* TRX

                =========================

                ❗️ Вы можете отправить только TRX сети TRC-20❗️

                ❗️ Минимальный депозит - 1 TRX❗️

                ⌛️ Среднее время зачисления депозита - 2 минуты."""
                .formatted(
                    personalDepositAddress,FormattingTools.formatBalance(personalSunBalance),
                    groupDepositAddress,FormattingTools.formatBalance(groupSunBalance)
                    );
    }

    public static String getDepositMenuText(String depositAddress, Long sunBalance) {
        return """
                💰 Ваш адресс депозита:

                `%s`

                💰 Баланс:

                *%s* TRX

                ❗️ Вы можете отправить только TRX сети TRC-20❗️

                ❗️ Минимальный депозит - 1 TRX❗️

                ⌛️ Среднее время зачисления депозита - 2 минуты."""
                .formatted(
                        depositAddress,
                        FormattingTools.formatBalance(sunBalance));
    }
}
