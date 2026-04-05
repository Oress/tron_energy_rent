package org.ipan.nrgyrent.telegram.i18n;

import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@AllArgsConstructor
public class AutoAmlLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    public String description(Locale locale) {
        return getLocalizedMessage("autoaml.description", locale);
    }

    public String thresholdPrompt(Locale locale) {
        return getLocalizedMessage("autoaml.threshold_prompt", locale);
    }

    public String sessionStartMessage(Locale locale, String walletAddress, String thresholdUsdt) {
        return getLocalizedMessage("autoaml.session_start_msg", locale, walletAddress, thresholdUsdt);
    }

    public String sessionStopMessage(Locale locale, String walletAddress) {
        return getLocalizedMessage("autoaml.session_stop_msg", locale, walletAddress);
    }

    public String sessionStopLowBalance(Locale locale, String walletAddress) {
        return getLocalizedMessage("autoaml.session_stop.low_balance", locale, walletAddress);
    }

    public String invalidThreshold(Locale locale) {
        return getLocalizedMessage("autoaml.invalid_threshold", locale);
    }

    public String sessionAlreadyExists(Locale locale) {
        return getLocalizedMessage("autoaml.session_already_exists", locale);
    }
}
