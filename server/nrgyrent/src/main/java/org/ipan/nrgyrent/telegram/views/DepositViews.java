package org.ipan.nrgyrent.telegram.views;

import org.ipan.nrgyrent.UsdtDepositConfig;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.telegram.i18n.DepositLabels;
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
    private final DepositLabels depositLabels;
    private final UsdtDepositConfig usdtDepositConfig;

    @Retryable
    @SneakyThrows
    public void updMenuToDepositsMenu(UserState userState, AppUser user) {
        Balance personalBalance = user.getBalance();
        Balance groupBalance = user.getGroupBalance();
        boolean usdtEnabled = usdtDepositConfig.isEnabled();

        String text = groupBalance != null
            ? getGroupDepositMenuText(groupBalance.getDepositAddress(), groupBalance.getSunBalance(), usdtEnabled)
            : getPersonalDepositMenuText(personalBalance.getDepositAddress(), personalBalance.getSunBalance(), usdtEnabled);

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

    public String getGroupDepositMenuText(String groupDepositAddress, Long groupSunBalance, boolean usdtEnabled) {
        String trx = FormattingTools.formatBalance(groupSunBalance);
        return usdtEnabled
                ? depositLabels.depositGroup(groupDepositAddress, trx)
                : depositLabels.depositGroupTrxOnly(groupDepositAddress, trx);
    }

    public String getPersonalDepositMenuText(String depositAddress, Long sunBalance, boolean usdtEnabled) {
        String trx = FormattingTools.formatBalance(sunBalance);
        return usdtEnabled
                ? depositLabels.depositPersonal(depositAddress, trx)
                : depositLabels.depositPersonalTrxOnly(depositAddress, trx);
    }
}
