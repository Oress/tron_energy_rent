package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class FormattingTools {
    private static DecimalFormat df = new DecimalFormat("# ###.##");

    public static String formatBalance(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)));
    }

    public static String formatNumber(Long number) {
        return df.format(number);
    }

    public static String formatNumber(Integer number) {
        return df.format(number);
    }

}
