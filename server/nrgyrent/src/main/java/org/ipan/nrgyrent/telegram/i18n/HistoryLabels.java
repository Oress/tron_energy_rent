package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class HistoryLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String historyMsg(String history) {
        return getLocalizedMessage("history.msg", history);
    }

    public String itemTx(String id, String a, Integer amnt, String sumTotal, String reciever, String status, String balance, String date) {
        return getLocalizedMessage("history.item.transaction", id, a, amnt, sumTotal, reciever, status, balance, date);
    }

    public String itemTxMember(String member) {
        return getLocalizedMessage("history.item.transaction.member", member);
    }

    public String itemWithdraw(String sumTotal, String reciever, String status, String balance, String date) {
        return getLocalizedMessage("history.item.withdraw", sumTotal, reciever, status, balance, date);
    }

    public String itemDeposit(String sumTotal, String sender, String balance, String date) {
        return getLocalizedMessage("history.item.deposit", sumTotal, sender, balance, date);
    }

    public String balanceGroup() {
        return getLocalizedMessage("history.balance_type.group");
    }

    public String balancePersonal() {
        return getLocalizedMessage("history.balance_type.personal");
    }
}
