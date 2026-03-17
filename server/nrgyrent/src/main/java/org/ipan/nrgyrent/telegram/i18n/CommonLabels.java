package org.ipan.nrgyrent.telegram.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class CommonLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    private String getLocalizedMessage(String key, Locale userLocale, Object... args) {
        return messageSource.getMessage(key, args, userLocale);
    }

    public String check() {
        return getLocalizedMessage("common.check");
    }

    public String cross() {
        return getLocalizedMessage("common.cross");
    }

    public String getMainWelcome(Locale localeOrDefault, String balanceLbl, String simpleTxPrice) {
        return getLocalizedMessage("main.welcome", localeOrDefault, balanceLbl, simpleTxPrice);
    }

    public String getCommonGroupBalance(Locale localeOrDefault, String trx) {
        return getLocalizedMessage("common.balance.group",localeOrDefault, trx);
    }

    public String getCommonPersonalBalance(Locale localeOrDefault, String trx) {
        return getLocalizedMessage("common.balance.personal", localeOrDefault, trx);
    }

    public String yes() {
        return getLocalizedMessage("common.yes");
    }

    public String no() {
        return getLocalizedMessage("common.no");
    }

    public String manageAdd() {
        return getLocalizedMessage("common.add_new");
    }

    // SEARCH START
    public String manageSearch() {
        return getLocalizedMessage("common.search");
    }

    public String searchNoResults() {
        return getLocalizedMessage("common.search_no_results");
    }

    public String searchResults() {
        return getLocalizedMessage("common.search_results");
    }

    public String searchNextPage() {
        return getLocalizedMessage("common.search_next_page");
    }

    public String searchPrevPage() {
        return getLocalizedMessage("common.search_prev_page");
    }

    public String searchReset() {
        return getLocalizedMessage("common.search_reset");
    }
    // SEARCH END
    
    public String toMainMenu() {
        return getLocalizedMessage("common.to_main_menu");
    }

    public String goBack() {
        return getLocalizedMessage("common.go_back");
    }

    // MAIN MENU START
    public String getTxType1(String amount) {
        return getLocalizedMessage("menu.tx_type1", amount);
    }

    public String getTxType2(String amount) {
        return getLocalizedMessage("menu.tx_type2", amount);
    }

    public String getTxCustomAmnt(String amount) {
        return getLocalizedMessage("menu.tx_custom_amnt", amount);
    }

    public String getEstimateTxCost() {
        return getLocalizedMessage("menu.estimate_tx_cost");
    }

    public String getAutoDelegation() {
        return getLocalizedMessage("menu.auto_delegation");
    }

    public String getMenuManageGroup() {
        return getLocalizedMessage("menu.manage_group");
    }

    public String getMenuManageReferals() {
        return getLocalizedMessage("menu.manage_referals");
    }

    public String getMenuWithdraw() {
        return getLocalizedMessage("menu.withdraw_trx");
    }

    public String getMenuDeposit() {
        return getLocalizedMessage("menu.deposit");
    }

    public String getMenuAdminMenu() {
        return getLocalizedMessage("menu.admin_menu");
    }

    public String getMenuTxHistory() {
        return getLocalizedMessage("menu.transactions_history");
    }

    public String menuSettings() {
        return getLocalizedMessage("menu.settings");
    }

    public String getMenuWallets() {
        return getLocalizedMessage("menu.wallets");
    }

    // END MAIN MENU

    public String settingsTxHistory() {
        return getLocalizedMessage("settings.transactions_history");
    }

    public String settingsChangeLanguage() {
        return getLocalizedMessage("settings.change_language");
    }

    public String settingsTopupHistory() {
        return getLocalizedMessage("settings.topup_history");
    }

    public String settingsDescription() {
        return getLocalizedMessage("settings.description");
    }

    public String settingsShowWalletsEnabled() {
        return getLocalizedMessage("settings.show_wallets_in_menu.enabled");
    }

    public String settingsShowWalletsDisabled() {
        return getLocalizedMessage("settings.show_wallets_in_menu.disabled");
    }

    public String somethingWentWrong() {
        return getLocalizedMessage("common.something_went_wrong");
    }

    public String topup(Locale loc, String trx) {
        return getLocalizedMessage("notification.topup", loc, trx);
    }

    public String topupUsdt(Locale loc, String usdt, String rate, String bybitFee, String resultTrx) {
        return getLocalizedMessage("notification.topup_usdt", loc, usdt, rate, bybitFee, resultTrx);
    }

    public String withdrawFail(Locale loc) {
        return getLocalizedMessage("notification.withdraw.fail", loc);
    }

    public String withdrawSuccess(Locale loc) {
        return getLocalizedMessage("notification.withdraw.success", loc);
    }

    public String userLogin(String login) {
        return getLocalizedMessage("user_format.login", login);
    }

    public String userName(String name) {
        return getLocalizedMessage("user_format.name", name);
    }

    public String groupLabel(String label) {
        return getLocalizedMessage("group_format.label", label);
    }

    public String defaultTariffWarning() {
        return getLocalizedMessage("warning.default_tariff");
    }

    public String historyWaiting() {
        return getLocalizedMessage("history.waiting");
    }

    public String historyComplete() {
        return getLocalizedMessage("history.success");
    }

    public String historyRefund() {
        return getLocalizedMessage("history.refund");
    }

    public String referalsSummary(String pendingCommission, String referalProgramDescr, String referals) {
        return getLocalizedMessage("referals.summary", pendingCommission, referalProgramDescr, referals);
    }

    public String referalPayment(Locale locale, String amount) {
        return getLocalizedMessage("referal.payment", locale, amount);
    }

    public String noReferrals() {
        return getLocalizedMessage("referal.no_referrals");
    }

    public String refProgramNotSet() {
        return getLocalizedMessage("referal.not_set");
    }

    public String greenCircle() {
        return "🟢";
    }

    public String redCircle() {
        return "🔴";
    }

    public String alertItrxBalanceLow(Locale locale, String currentBalance) {
        return getLocalizedMessage("alerts.itrx_balance_low", locale, currentBalance);
    }

    public String autoDelegationAlertBalanceLowUser(Locale locale, String currentBalance) {
        return getLocalizedMessage("alerts.auto_delegation_low_balance", locale, currentBalance);
    }

    public String autoDelegationAlertBalanceLowAdmin(Locale locale, String currentBalance, String user) {
        return getLocalizedMessage("alerts.auto_delegation_low_balance.admin", locale, currentBalance, user);
    }

    public String alertNettsBalanceLow(Locale locale, String currentBalance) {
        return getLocalizedMessage("alerts.netts_balance_low", locale, currentBalance);
    }

    public String alertTrxxBalanceLow(Locale locale, String currentBalance) {
        return getLocalizedMessage("alerts.trxx_balance_low", locale, currentBalance);
    }

    public String alertCatfeeBalanceLow(Locale locale, String currentBalance) {
        return getLocalizedMessage("alerts.catfee_balance_low", locale, currentBalance);
    }

    public String getAmlCheck() {
        return getLocalizedMessage("menu.aml_check");
    }

    public String amlMenuDescription(Locale locale, String price) {
        return getLocalizedMessage("aml.menu_description", locale, price);
    }

    public String amlMenuCheckWallet() {
        return getLocalizedMessage("aml.menu_check_wallet");
    }

    public String amlMenuHistory() {
        return getLocalizedMessage("aml.menu_history");
    }

    public String amlPromptWallet(Locale locale, String price, String balance) {
        return getLocalizedMessage("aml.prompt_wallet", locale, price, balance);
    }

    public String amlInsufficientBalance(Locale locale, String price, String balance) {
        return getLocalizedMessage("aml.insufficient_balance", locale, price, balance);
    }

    public String amlRequestReceived(Locale locale, String walletAddress) {
        return getLocalizedMessage("aml.request_received", locale, walletAddress);
    }

    public String amlReportFailed(Locale locale, String walletAddress) {
        return getLocalizedMessage("aml.report_failed", locale, walletAddress);
    }

    public String amlHistoryEmpty(Locale locale) {
        return getLocalizedMessage("aml.history_empty", locale);
    }

    public String amlHistoryHeader(Locale locale) {
        return getLocalizedMessage("aml.history.header", locale);
    }

    public String amlReportHeader(Locale locale) {
        return getLocalizedMessage("aml.report.header", locale);
    }

    public String amlReportAddress(Locale locale) {
        return getLocalizedMessage("aml.report.address", locale);
    }

    public String amlReportRiskSummary(Locale locale) {
        return getLocalizedMessage("aml.report.risk_summary", locale);
    }

    public String amlReportRiskScore(Locale locale, String score) {
        return getLocalizedMessage("aml.report.risk_score", locale, score);
    }

    public String amlReportRiskLevel(Locale locale, String level) {
        return getLocalizedMessage("aml.report.risk_level", locale, level);
    }

    public String amlReportSanctioned(Locale locale, String value) {
        return getLocalizedMessage("aml.report.sanctioned", locale, value);
    }

    public String amlReportSanctionedYes(Locale locale) {
        return getLocalizedMessage("aml.report.sanctioned_yes", locale);
    }

    public String amlReportSanctionedNo(Locale locale) {
        return getLocalizedMessage("aml.report.sanctioned_no", locale);
    }

    public String amlReportFundExposure(Locale locale) {
        return getLocalizedMessage("aml.report.fund_exposure", locale);
    }

    public String amlReportExposureSource(Locale locale) {
        return getLocalizedMessage("aml.report.exposure_source", locale);
    }

    public String amlReportRisksHeader(Locale locale) {
        return getLocalizedMessage("aml.report.risks_header", locale);
    }

    public String amlReportComputed(Locale locale, String dateTime) {
        return getLocalizedMessage("aml.report.computed", locale, dateTime);
    }

    public String amlReportProvider(Locale locale, String providerName) {
        return getLocalizedMessage("aml.report.provider", locale, providerName);
    }

    public String amlCategoryName(Locale locale, String category) {
        if (category == null) return "Unknown";
        String key = "aml.category." + category.toLowerCase().replace(" ", "_");
        return messageSource.getMessage(key, null, category, locale);
    }

    public String amlRiskLevelName(Locale locale, org.ipan.nrgyrent.domain.model.AmlRiskLevel level) {
        if (level == null) return "N/A";
        String key = "aml.risk_level." + level.name().toLowerCase();
        return messageSource.getMessage(key, null, level.name(), locale);
    }

    public String amlProximity(Locale locale, String proximity) {
        if (proximity == null) return "";
        String key = "aml.proximity." + proximity.toLowerCase();
        return messageSource.getMessage(key, null, proximity, locale);
    }
}
