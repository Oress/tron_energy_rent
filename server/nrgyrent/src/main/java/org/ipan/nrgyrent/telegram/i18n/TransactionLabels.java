package org.ipan.nrgyrent.telegram.i18n;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class TransactionLabels {
    private final MessageSource messageSource;
    private final TgUserLocaleProvider tgUserLocaleProvider;

    private String getLocalizedMessage(String key, Object... args) {
        Locale userLocale = tgUserLocaleProvider.getUserLocale();
        return messageSource.getMessage(key, args, userLocale);
    }

    private String getLocalizedMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

    public String notEnoughtBalance() {
        return getLocalizedMessage("transactions.error.not_enough_balance");
    }

    public String notEnoughtBalanceAutodelegateReserve(String amount) {
        return getLocalizedMessage("transactions.error.not_enough_balance_autodelegate_reserve", amount);
    }

    public String inProgress() {
        return getLocalizedMessage("transactions.in_progress");
    }

    public String pending() {
        return getLocalizedMessage("transactions.pending");
    }

    public String customAmountPromptAmount(String price) {
        return getLocalizedMessage("transactions.manual_amount_tx.prompt_amount", price);
    }

    public String customAmountPromptAddress(Integer amount,String price, BigDecimal total) {
        return getLocalizedMessage("transactions.manual_amount_tx.prompt_address", amount, price, total);
    }

    public String tx1PromptAddress(String price) {
        return getLocalizedMessage("transactions.tx_type1.prompt_address", price);
    }

    public String tx2PromptAddress(String price) {
        return getLocalizedMessage("transactions.tx_type2.prompt_address", price);
    }

    public String somethingWrong(String id) {
        return getLocalizedMessage("transactions.error.something_went_wrong_w_order", id);
    }

    public String walletNotActive(String id) {
        return getLocalizedMessage("transactions.error.wallet_not_active", id);
    }

    public String itrxOutOfTrx(String id) {
        return getLocalizedMessage("transactions.error.itrx_out_of_trx", id);
    }

    public String successIndividual(Locale locale, Integer amount, String total, String receiver, String balance, String date) {
        return getLocalizedMessage("transactions.success.individual", locale, amount, total, receiver, balance, date);
    }

    public String successGroup(Locale locale, Integer amount, String total, String receiver, String date) {
        return getLocalizedMessage("transactions.success.group", locale, amount, total, receiver, date);
    }

    public String refunded(Locale locale, Integer amount, String total, String receiver) {
        return getLocalizedMessage("transactions.refunded", locale, amount, total, receiver);
    }

}
