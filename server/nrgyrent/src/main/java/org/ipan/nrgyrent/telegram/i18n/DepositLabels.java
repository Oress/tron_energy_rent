package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class DepositLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String depositGroup(String address, String trx) {
        return getLocalizedMessage("deposits.manage_group", address, trx);
    }

    public String depositPersonal(String address, String trx) {
        return getLocalizedMessage("deposits.manage_personal", address, trx);
    }

}
