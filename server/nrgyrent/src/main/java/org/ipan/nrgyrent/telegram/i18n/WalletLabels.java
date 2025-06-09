package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class WalletLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String getWalletsManage() {
        return getLocalizedMessage("wallets.manage");
    }

    public String deleteWallet() {
        return getLocalizedMessage("wallets.delete");
    }

    public String addWallet() {
        return getLocalizedMessage("wallets.add");
    }

    public String deleteWalletSuccess() {
        return getLocalizedMessage("wallets.delete.success");
    }

    public String addWalletSuccess() {
        return getLocalizedMessage("wallets.add.success");
    }

    public String promptAddress() {
        return getLocalizedMessage("wallets.prompt_address");
    }

    public String promptLabel() {
        return getLocalizedMessage("wallets.prompt_label");
    }

    public String item(String label, String address) {
        return getLocalizedMessage("wallets.item", label, address);
    }
}
