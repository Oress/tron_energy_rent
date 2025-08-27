package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@Component
@AllArgsConstructor
public class RefProgramLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String refProgramDescription(String label, String percentage, String link) {
        return getLocalizedMessage("user.ref_program.description", label, percentage, link);
    }

    public String refProgramDescriptionWoDescription(String percentage, String link) {
        return getLocalizedMessage("user.ref_program.descriptionwolabel", percentage, link);
    }

    public String manage() {
        return getLocalizedMessage("admin.referal.manage");
    }

    public String newPromptLabel() {
        return getLocalizedMessage("admin.referal.new.prompt_label");
    }

    public String newPromptPercentage() {
        return getLocalizedMessage("admin.referal.new.prompt_percentage");
    }

    public String newPromptBaseTxValue() {
        return getLocalizedMessage("admin.referal.new.base_tx_value");
    }

    public String newSuccess() {
        return getLocalizedMessage("admin.referal.new.success");
    }

    public @NonNull String actionsRenameSuccess() {
        return getLocalizedMessage("admin.referal.actions.rename.success");
    }

    public String changeBaseAmountSuccess() {
        return getLocalizedMessage("admin.referal.actions.change_base_amount.success");
    }

    public @NonNull String actionsRename() {
        return getLocalizedMessage("admin.referal.actions.rename");
    }

    public String disableSebes() {
        return getLocalizedMessage("admin.referal.actions.disabled_sebes");
    }

    public String enableSebes() {
        return getLocalizedMessage("admin.referal.actions.enabled_sebes");
    }

    public String changeTx1SubtractAmount() {
        return getLocalizedMessage("admin.referal.actions.change_tx1_subtract_amount");
    }

    public String changeTx2SubtractAmount() {
        return getLocalizedMessage("admin.referal.actions.change_tx2_subtract_amount");
    }

    public String changeTx1AutoSubtractAmount() {
        return getLocalizedMessage("admin.referal.actions.change_tx1_auto_subtract_amount");
    }

    public String changeTx2AutoSubtractAmount() {
        return getLocalizedMessage("admin.referal.actions.change_tx2_auto_subtract_amount");
    }

    public @NonNull String actionsChangePercentage() {
        return getLocalizedMessage("admin.referal.actions.change_percentage");
    }

    public String actionsPreview(String label, Integer percentage, String calcType, String isPredefined,
                                 String formatDateToUtc,
                                 String tx1BaseAmount,
                                 String tx2BaseAmount,
                                 String tx1AutoBaseAmount,
                                 String tx2AutoBaseAmount
                                 ) {
        return getLocalizedMessage("admin.referal.preview", label, percentage, calcType, isPredefined,
                formatDateToUtc, tx1BaseAmount, tx2BaseAmount, tx1AutoBaseAmount, tx2AutoBaseAmount);
    }

    public String percent_from_revenue() {
        return getLocalizedMessage("admin.referal.calc_type.percent_from_revenue");
    }

    public String percent_from_profit() {
        return getLocalizedMessage("admin.referal.calc_type.percent_from_profit");
    }

    public @NonNull String actionsRenamePromptLabel() {
        return getLocalizedMessage("admin.referal.actions.rename.prompt");
    }

    public @NonNull String actionsRenameLabelTooShort() {
        return getLocalizedMessage("admin.referal.actions.rename.label_too_short");
    }

    public @NonNull String actionsChangePercentageSuccess() {
        return getLocalizedMessage("admin.referal.actions.change_percentage.success");
    }
}
