package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class BalanceTools {
    private static DecimalFormat df = new DecimalFormat("# ###.##");

    public static String formatBalance(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)));
    }

}
