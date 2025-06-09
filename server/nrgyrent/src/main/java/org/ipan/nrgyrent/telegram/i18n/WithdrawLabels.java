package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@Component
@AllArgsConstructor
public class WithdrawLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String promptWallet() {
        return getLocalizedMessage("withdraw.prompt_wallet");
    }

    public String inProgress() {
        return getLocalizedMessage("withdraw.in_progress");
    }

    public String notEnoughRights() {
        return getLocalizedMessage("withdraw.not_enough_rights");
    }

    public String fail() {
        return getLocalizedMessage("withdraw.fail");
    }

    public String success() {
        return getLocalizedMessage("withdraw.success");
    }

    public String notEnoughtBalance() {
        return getLocalizedMessage("withdraw.not_enough_balance");
    }

    public String serviceNotEnoughtBalance() {
        return getLocalizedMessage("withdraw.service_not_enough_balance");
    }

    public String promptNotEnoughtBalance(String formatBalance) {
        return getLocalizedMessage("withdraw.prompt_not_enough_balance", formatBalance);
    }

    public String promptAllowedToWithdraw(String formatBalance) {
        return getLocalizedMessage("withdraw.allowed_to_withdraw", formatBalance);
    }

    public @NonNull String transactionToInactiveWallet() {
        return getLocalizedMessage("withdraw.inactive_wallet");
    }
}
