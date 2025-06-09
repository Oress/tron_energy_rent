package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TariffLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String manage() {
        return getLocalizedMessage("tariff.manage");
    }

    public String menuChangeTx1Amount() {
        return getLocalizedMessage("tariff.change_tx1_amount");
    }

    public String menuChangeTx2Amount() {
        return getLocalizedMessage("tariff.change_tx2_amount");
    }

    public String menuRename() {
        return getLocalizedMessage("tariff.rename");
    }

    public String menuDeactivate() {
        return getLocalizedMessage("tariff.deactivate");
    }

    public String warnDeactivate() {
        return getLocalizedMessage("tariff.deactivate_warning");
    }

    public String deactivateSuccess() {
        return getLocalizedMessage("tariff.deactivate_success");
    }

    public String promptNewLabel() {
        return getLocalizedMessage("tariff.prompt_new_label");
    }

    public String renameSuccess() {
        return getLocalizedMessage("tariff.rename_success");
    }

    public String amountChangeSuccess() {
        return getLocalizedMessage("tariff.amount_change_success");
    }

    public String warnTariffLabelShort() {
        return getLocalizedMessage("tariff.tariff_too_short");
    }

    public String addSuccess() {
        return getLocalizedMessage("tariff.add_success");
    }

    public String promptLabel() {
        return getLocalizedMessage("tariff.prompt_label");
    }

    public String promptTx1Amount() {
        return getLocalizedMessage("tariff.prompt_tx1_amount");
    }

    public String promptTx2Amount() {
        return getLocalizedMessage("tariff.prompt_tx2_amount");
    }

    public String preview(String label, String predef, String amnt1, String amnt2, String createdDt, String isActive, String a) {
        return getLocalizedMessage("tariff.preview", label, predef, amnt1, amnt2, createdDt, isActive, a);
    }
}
