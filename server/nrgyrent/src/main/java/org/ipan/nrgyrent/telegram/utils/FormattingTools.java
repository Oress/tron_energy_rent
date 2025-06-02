package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;

public class FormattingTools {
    private static DecimalFormat df = new DecimalFormat("# ###.##");

    public static String formatUserLink(TgUserId user) {
        if (user == null) {
            return "-";
        }
        if (user.getUsername() != null) {
            return String.format("[@%s](https://t.me/%s)", user.getUsername(), user.getUsername());
        } else {
            return String.format("%s %s (логин не задан)", user.getId(), user.getFirstName());
        }
    }

    public static String formatUserLink(AppUser user) {
        if (user == null) {
            return "-";
        }
        return String.format("[@%s](https://t.me/%s) %s", user.getTelegramUsername(), user.getTelegramUsername(), user.getTelegramFirstName());
    }

    public static String formatUser(AppUser user) {
        if (user == null) {
            return "-";
        }
        return String.format("%s %s", user.getTelegramUsername(), user.getTelegramFirstName());
    }

    public static String formatBalance(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)));
    }

    public static String formatBalanceTrx(BigDecimal balanceTrx) {
        return df.format(balanceTrx);
    }

    public static String formatDateToUtc(Instant date) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(java.time.ZoneOffset.UTC)
            .format(date);
    }


    public static String formatDateToUtc(Timestamp date) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(java.time.ZoneOffset.UTC)
            .format(LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.systemDefault()));
    }

    public static String orderStatusLabel(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING -> "⏳ Ожидание";
            case COMPLETED -> "✅ Завершено";
            case REFUNDED -> "❌ Возврат";
        };
    }

    public static String withdrawalStatusLabel(WithdrawalStatus withdrawalStatus) {
        return switch (withdrawalStatus) {
            case PENDING -> "⏳ Ожидание";
            case COMPLETED -> "✅ Завершено";
            case FAILED -> "❌ Возврат";
        };
    }

    public static String formatNumber(Long number) {
        return df.format(number);
    }

    public static String formatNumber(Integer number) {
        return df.format(number);
    }

}
