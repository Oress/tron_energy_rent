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


    public String manage() {
        return getLocalizedMessage("admin.referal.manage");
    }

    public String newPromptLabel() {
        return getLocalizedMessage("admin.referal.new.prompt_label");
    }

    public String newPromptPercentage() {
        return getLocalizedMessage("admin.referal.new.prompt_percentage");
    }

    public String newSuccess() {
        return getLocalizedMessage("admin.referal.new.success");
    }

    public @NonNull String actionsRenameSuccess() {
        return getLocalizedMessage("admin.referal.actions.rename.success");
    }

    public @NonNull String actionsRename() {
        return getLocalizedMessage("admin.referal.actions.rename");
    }

    public @NonNull String actionsChangePercentage() {
        return getLocalizedMessage("admin.referal.actions.change_percentage");
    }

    public String actionsPreview(String label, Integer percentage, String formatDateToUtc) {
        return getLocalizedMessage("admin.referal.preview", label, percentage, formatDateToUtc);
    }

    public @NonNull String actionsRenamePromptLabel() {
        return getLocalizedMessage("admin.referal.actions.rename.prompt_label");
    }

    public @NonNull String actionsRenameLabelTooShort() {
        return getLocalizedMessage("admin.referal.actions.rename.label_too_short");
    }

    public @NonNull String actionsChangePercentageSuccess() {
        return getLocalizedMessage("admin.referal.actions.change_percentage.success");
    }
}
