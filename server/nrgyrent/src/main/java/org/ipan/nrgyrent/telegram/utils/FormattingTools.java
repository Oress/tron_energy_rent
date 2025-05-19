package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.ipan.nrgyrent.domain.model.OrderStatus;

public class FormattingTools {
    private static DecimalFormat df = new DecimalFormat("# ###.##");

    public static String formatBalance(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)));
    }

    public static String formatDateToUtc(Instant date) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(java.time.ZoneOffset.UTC)
            .format(date);
    }

    public static String orderStatusLabel(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING -> "⏳ Ожидание";
            case COMPLETED -> "✅ Завершено";
            case REFUNDED -> "❌ Возврат";
        };
    }

    public static String formatNumber(Long number) {
        return df.format(number);
    }

    public static String formatNumber(Integer number) {
        return df.format(number);
    }

}
