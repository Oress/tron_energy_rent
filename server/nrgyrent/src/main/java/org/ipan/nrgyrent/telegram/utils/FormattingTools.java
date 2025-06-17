package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.WithdrawalStatus;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.RefProgramLabels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FormattingTools {
    private static final DecimalFormat df = new DecimalFormat("# ###.##");

    private final CommonLabels commonLabels;
    private final RefProgramLabels refProgramLabels;
    private final String botLogin;

    public FormattingTools(
        @Value("${app.bot.username:tron_energy_rent_dev_bot}")String botLogin,
        RefProgramLabels refProgramLabels,
        CommonLabels commonLabels) {
        this.commonLabels = commonLabels;
        this.refProgramLabels = refProgramLabels;
        this.botLogin = botLogin;
    }

    public static String valOrDash(String val) {
        return val == null ? "-": val;
    }

    public String formatUserForSearch(Long id, String login, String name) {
        String loginStr = login != null ? commonLabels.userLogin(login) : "";
        String nameStr = name != null ? commonLabels.userLogin(name) : "";
        String idStr = "ID: %s".formatted(id);
        return List.of(idStr, loginStr, nameStr).stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
    }

    public String formatUserForSearch(AppUser user) {
        if (user == null) {
            return "-";
        }
        String login = user.getTelegramUsername() != null ? commonLabels.userLogin(user.getTelegramUsername()) : "";
        String name = user.getTelegramFirstName() != null ? commonLabels.userName(user.getTelegramFirstName()) : "";
        String id = "ID: %s".formatted(user.getTelegramId());
        return List.of(login, name, id).stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
    }

    public static String formatUserLink(TgUserId user) {
        if (user == null) {
            return "-";
        }
        if (user.getUsername() != null) {
            return String.format("[@%s](https://t.me/%s)", user.getUsername(), user.getUsername());
        } else {
            return String.format("%s %s", user.getId(), user.getFirstName());
        }
    }

    public String formatStartLink(String startParam) {
        return String.format("https://t.me/%s?start=%s", botLogin, startParam);
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
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)).setScale(2, RoundingMode.HALF_DOWN));
    }

    public String formatRefProgmam(BalanceReferralProgram refProgram) {
        return refProgramLabels.refProgramDescription(
                refProgram.getReferralProgram().getLabel(),
                refProgram.getReferralProgram().getPercentage().toString(),
                formatStartLink(refProgram.getLink()));
    }

    public String formatRefProgmamWoDescription(BalanceReferralProgram refProgram) {
        return refProgramLabels.refProgramDescriptionWoDescription(
                refProgram.getReferralProgram().getPercentage().toString(),
                formatStartLink(refProgram.getLink()));
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

    public String orderStatusLabel(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING -> commonLabels.historyWaiting();
            case COMPLETED -> commonLabels.historyComplete();
            case REFUNDED -> commonLabels.historyRefund();
        };
    }

    public String withdrawalStatusLabel(WithdrawalStatus withdrawalStatus) {
        return switch (withdrawalStatus) {
            case PENDING -> commonLabels.historyWaiting();
            case COMPLETED -> commonLabels.historyComplete();
            case FAILED -> commonLabels.historyRefund();
        };
    }

    public static String formatNumber(Long number) {
        return df.format(number);
    }

    public static String formatNumber(Integer number) {
        return df.format(number);
    }

}
