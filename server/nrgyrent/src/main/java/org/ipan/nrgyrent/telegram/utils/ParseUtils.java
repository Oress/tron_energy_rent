package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ipan.nrgyrent.itrx.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParseUtils {
    @Value("${app.rounding.scale:2}")
    private Integer scale;

    public Long parseTrxStrToSunLong(String trxStr) {
        try {
            if (trxStr != null) {
                trxStr = trxStr.replace(",", ".");
            }
            BigDecimal trxAmount = new BigDecimal(trxStr).setScale(scale, RoundingMode.HALF_DOWN);
            BigDecimal sunAmount = trxAmount.multiply(AppConstants.trxToSunRate);

            long sunAmountLong = sunAmount.longValue();
            return sunAmountLong;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid TRX amount format: " + trxStr, e);
        }
    }

    public static String escapeMarkdown(String text) {
        return text
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("`", "\\`")
        .replace("[", "\\[")
        .replace("]", "\\]");
    }

    public static String escapeMarkdown2(String text) {
        return text
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace("~", "\\~")
        .replace("`", "\\`")
        .replace(">", "\\>")
        .replace("#", "\\#")
        .replace("+", "\\+")
        .replace("-", "\\-")
        .replace("=", "\\=")
        .replace("|", "\\|")
        .replace("{", "\\{")
        .replace("}", "\\}")
        .replace(".", "\\.")
        .replace("!", "\\!");
    }
}
