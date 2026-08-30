package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class ManageUserLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    public String preview(String telegramId, String valOrDash, String valOrDash2, String tariffLabel, Object object,
            String depositAddress, String formatBalance, String withdrawLimitTotal, String withdrawLimitRemaining, String refferalProgram) {
        return getLocalizedMessage("manage_users.preview", telegramId, valOrDash, valOrDash2, tariffLabel, object, depositAddress, formatBalance,
                withdrawLimitTotal, withdrawLimitRemaining, refferalProgram);
    }

    public String menuChangeTariff() {
        return getLocalizedMessage("manage_users.menu.change_tariff");
    }

    public String menuChangeRefProgram() {
        return getLocalizedMessage("manage_users.menu.change_ref_program");
    }

    public String menuChangeBalance() {
        return getLocalizedMessage("manage_users.menu.change_balance");
    }

    public String menuChangeWithdrawLimit() {
        return getLocalizedMessage("manage_users.menu.change_withdraw_limit");
    }

    public String menuDeactivate() {
        return getLocalizedMessage("manage_users.menu.deactivate");
    }

    public String menuAutoDelegation() {
        return getLocalizedMessage("manage_users.menu.auto_delegation");
    }

    public String autoDelegationDescription() {
        return getLocalizedMessage("manage_users.auto_delegation.description");
    }

    public String autoDelegationWarningActiveUnaffected() {
        return getLocalizedMessage("manage_users.auto_delegation.warning_active_unaffected");
    }

    public String autoDelegationConfiguredProvider(String provider) {
        return getLocalizedMessage("manage_users.auto_delegation.configured_provider", provider);
    }

    public String autoDelegationEffectiveProvider(String provider) {
        return getLocalizedMessage("manage_users.auto_delegation.effective_provider", provider);
    }

    public String autoDelegationDefaultProvider() {
        return getLocalizedMessage("manage_users.auto_delegation.default_provider");
    }

    public String autoDelegationUseDefault() {
        return getLocalizedMessage("manage_users.auto_delegation.use_default");
    }

    public String autoDelegationActiveSessions() {
        return getLocalizedMessage("manage_users.auto_delegation.active_sessions");
    }

    public String autoDelegationNoActiveSessions() {
        return getLocalizedMessage("manage_users.auto_delegation.no_active_sessions");
    }

    public String autoDelegationSession(String sessionId, String address, String provider, String createdAt) {
        return getLocalizedMessage("manage_users.auto_delegation.session", sessionId, address, provider, createdAt);
    }

    public String autoDelegationChangeProvider(String provider) {
        return getLocalizedMessage("manage_users.auto_delegation.change_provider", provider);
    }

    public String autoDelegationDeactivate() {
        return getLocalizedMessage("manage_users.auto_delegation.deactivate");
    }

    public String autoDelegationSwitchConfirm(String provider) {
        return getLocalizedMessage("manage_users.auto_delegation.switch_confirm", provider);
    }

    public String autoDelegationDeactivateConfirm() {
        return getLocalizedMessage("manage_users.auto_delegation.deactivate_confirm");
    }

    public String autoDelegationActionFailed() {
        return getLocalizedMessage("manage_users.auto_delegation.action_failed");
    }

    public String deactivateConfirm() {
        return getLocalizedMessage("manage_users.deactivate.confirm");
    }

    public String deactivateSuccess() {
        return getLocalizedMessage("manage_users.deactivate.success");
    }

    public String changeBalancePromptAmount() {
        return getLocalizedMessage("manage_users.change_balance.prompt_amount");
    }

    public String changeWithdrawLimitPromptAmount() {
        return getLocalizedMessage("manage_users.withdraw_limit.prompt_amount");
    }

    public String changeRefProgramSuccess() {
        return getLocalizedMessage("manage_users.change_ref_program.success");
    }

    public String changeBalanceNegative() {
        return getLocalizedMessage("manage_users.change_balance.negative");
    }

    public String changeWithdrawLimitNegative() {
        return getLocalizedMessage("manage_users.change_withdraw_limit.negative");
    }

    public String changeBalanceSuccess() {
        return getLocalizedMessage("manage_users.change_balance.success");
    }

    public String changeWithdrawSuccess() {
        return getLocalizedMessage("manage_users.change_withdraw.success");
    }

    public String changeTariffSuccess() {
        return getLocalizedMessage("manage_users.change_tariff.success");
    }
}
