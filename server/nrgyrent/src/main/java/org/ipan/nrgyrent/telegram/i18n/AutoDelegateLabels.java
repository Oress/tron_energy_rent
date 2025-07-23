package org.ipan.nrgyrent.telegram.i18n;

import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@AllArgsConstructor
public class AutoDelegateLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    private String getLocalizedMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    public String sessionStartMessage(Locale locale, String walletAddress) {
        return getLocalizedMessage("autodelegate.session_start_msg", locale, walletAddress);
    }

    public String sessionStopManually(Locale locale, String walletAddress) {
        return getLocalizedMessage("autodelegate.session_stop.manually", locale, walletAddress);
    }

    public String cannotStartSessionLowBalance(Locale locale) {
        return getLocalizedMessage("autodelegate.session_start.low_balance", locale);
    }

    public String sessionStopLowBalance(Locale locale, String walletAddress) {
        return getLocalizedMessage("autodelegate.session_stop.low_balance", locale, walletAddress);
    }

    public String sessionStopInactivity(Locale locale, String walletAddress) {
        return getLocalizedMessage("autodelegate.session_stop.inactivity", locale, walletAddress);
    }


    public String sessionStatusMessage(Locale locale, String s, String statusText, String s1) {
        return getLocalizedMessage("autodelegate.session_status_msg", locale, s, statusText, s1);
    }

    public String transactionSuccess(Locale locale, String amount, String receiveAddress) {
        return getLocalizedMessage("autodelegate.transactions.success", locale, amount, receiveAddress);
    }

    public String eventSessionStopped(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.event.session_stopped", localeOrDefault);
    }

    public String eventScInvocation(Locale localeOrDefault, String trx) {
        return getLocalizedMessage("autodelegate.event.sc_invocation", localeOrDefault, trx);
    }

    public String eventRedelegate(Locale localeOrDefault, String trx) {
        return getLocalizedMessage("autodelegate.event.redelegate", localeOrDefault, trx);
    }

    public String eventInitDelegate(Locale localeOrDefault, String trx) {
        return getLocalizedMessage("autodelegate.event.init_delegate", localeOrDefault, trx);
    }

    public String description(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.description", localeOrDefault);
    }

    public String unableToStartSessionDueToSystemOffline() {
        return getLocalizedMessage("autodelegate.unable_to_start");
    }

    public String unableToStartSessionDueToInitProblem() {
        return getLocalizedMessage("autodelegate.unable_to_start");
    }

    public String pending(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.order_pending", localeOrDefault);
    }

    public String completed(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.order_completed", localeOrDefault);
    }

    public String refunded(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.order_refunded", localeOrDefault);
    }

    public String statusActive(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.active", localeOrDefault);
    }

    public String statusStoppedByUser(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.stop_by_user", localeOrDefault);
    }

    public String statusEnergyUnused(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.energy_unused", localeOrDefault);
    }

    public String statusSystemOffline(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.system_offline", localeOrDefault);
    }

    public String statusInsufficientFunds(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.insuff_funds", localeOrDefault);
    }

    public String statusInactiveWallet(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.inactive_wallet", localeOrDefault);
    }

    public String statusSystemRestart(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.system_restart", localeOrDefault);
    }

    public String statusError(Locale localeOrDefault) {
        return getLocalizedMessage("autodelegate.status.error", localeOrDefault);
    }
}
