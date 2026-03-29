package org.ipan.nrgyrent.telegram.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.projections.ReferralDto;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.netts.dto.BitokResultResponse;
import org.ipan.nrgyrent.netts.dto.EllipticResultResponse;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.RefProgramLabels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FormattingTools {
    private static final DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
    private static final DecimalFormat df = new DecimalFormat("# ###.###");
    private static final Gson GSON = new GsonBuilder().create();

    // All known BitOK exposure category identifiers (snake_case from API and display names)
    private static final Map<String, String> CATEGORY_ICONS = Map.ofEntries(
            Map.entry("exchange", "🏦"),          Map.entry("Exchange", "🏦"),
            Map.entry("high_risk_exchange", "⚠️"), Map.entry("High Risk Exchange", "⚠️"),
            Map.entry("dex", "🔄"),               Map.entry("Dex", "🔄"),
            Map.entry("p2p_exchange", "🤝"),       Map.entry("P2p Exchange", "🤝"),
            Map.entry("gambling", "🎰"),           Map.entry("Gambling", "🎰"),
            Map.entry("sanctions", "🚫"),          Map.entry("Sanctions", "🚫"),
            Map.entry("stolen_funds", "💸"),       Map.entry("Stolen Funds", "💸"),
            Map.entry("ransomware", "🔒"),         Map.entry("Ransomware", "🔒"),
            Map.entry("darknet_market", "🕷"),     Map.entry("Darknet Market", "🕷"),
            Map.entry("terrorist_financing", "☠️"),Map.entry("Terrorist Financing", "☠️"),
            Map.entry("scam", "💀"),               Map.entry("Scam", "💀"),
            Map.entry("fraud_shop", "🏴"),         Map.entry("Fraud Shop", "🏴"),
            Map.entry("illegal_service", "⛔"),    Map.entry("Illegal Service", "⛔"),
            Map.entry("enforcement_action", "🏛"), Map.entry("Enforcement Action", "🏛"),
            Map.entry("seized_funds", "🔐"),       Map.entry("Seized Funds", "🔐"),
            Map.entry("high_risk_jurisdiction", "🌍"), Map.entry("High Risk Jurisdiction", "🌍"),
            Map.entry("privacy_protocol", "🔀"),   Map.entry("Privacy Protocol", "🔀"),
            Map.entry("bridge", "🌉"),             Map.entry("Bridge", "🌉"),
            Map.entry("lending", "💰"),            Map.entry("Lending", "💰"),
            Map.entry("smart_contract", "📋"),     Map.entry("Smart Contract", "📋"),
            Map.entry("token_contract", "🪙"),     Map.entry("Token Contract", "🪙"),
            Map.entry("payment_service_provider", "💳"), Map.entry("Payment Service Provider", "💳"),
            Map.entry("custodial_wallet", "👜"),   Map.entry("Custodial Wallet", "👜"),
            Map.entry("mining_pool", "⛏"),        Map.entry("Mining Pool", "⛏"),
            Map.entry("atm", "🏧"),               Map.entry("Atm", "🏧"),
            Map.entry("iaas", "☁️"),              Map.entry("Iaas", "☁️"),
            Map.entry("dust", "🌫"),              Map.entry("Dust", "🌫"),
            Map.entry("unnamed_service", "👤"),    Map.entry("Unnamed Service", "👤"),
            Map.entry("other", "📁"),             Map.entry("Other", "📁"),
            Map.entry("Others", "📁")
    );

    public static String categoryIcon(String category) {
        if (category == null) return "📁";
        String icon = CATEGORY_ICONS.get(category);
        if (icon != null) return icon;
        // Try title-cased version of snake_case (e.g., "high_risk_exchange" → "High Risk Exchange")
        String titleCased = toTitleCase(category.replace("_", " "));
        return CATEGORY_ICONS.getOrDefault(titleCased, "📁");
    }

    public static String riskLevelEmoji(AmlRiskLevel level) {
        if (level == null) return "";
        return switch (level) {
            case NONE -> "⚪";
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🔴";
        };
    }

    private static String toTitleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public String formatAmlHistoryItemLabel(AmlVerification v, Locale locale) {
        String addr = v.getWalletAddress();
        String shortAddr = (addr != null && addr.length() > 12)
                ? addr.substring(0, 6) + "…" + addr.substring(addr.length() - 4)
                : (addr != null ? addr : "?");
        String emoji = riskLevelEmoji(v.getRiskLevel());
        if (v.getRiskLevel() != null) {
            return emoji + " " + shortAddr + " [" + commonLabels.amlRiskLevelName(locale, v.getRiskLevel()) + "]";
        }
        String status = commonLabels.amlPaymentStatusName(locale, v.getPaymentStatus());
        return shortAddr + " [" + status + "]";
    }

    public String formatAmlReport(AmlVerification v, Locale locale) {
        if (AmlProvider.ELLIPTIC.equals(v.getProvider())) {
            return formatEllipticAmlReport(v, locale);
        }
        return formatBitokAmlReport(v, locale);
    }

    private String formatBitokAmlReport(AmlVerification v, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(commonLabels.amlReportHeader(locale)).append("\n");
        sb.append(commonLabels.amlReportAddress(locale)).append(" `").append(v.getWalletAddress()).append("`\n\n");

        sb.append(commonLabels.amlReportRiskSummary(locale)).append("\n");
        String riskScore = v.getRiskScore() != null ? String.format("%.2f%%", v.getRiskScore() * 100) : "N/A";
        sb.append(commonLabels.amlReportRiskScore(locale, riskScore)).append("\n");

        String riskEmoji = riskLevelEmoji(v.getRiskLevel());
        String riskLevelName = commonLabels.amlRiskLevelName(locale, v.getRiskLevel());
        sb.append(commonLabels.amlReportRiskLevel(locale, riskEmoji + " " + riskLevelName)).append("\n");

        String sanctionedVal = Boolean.TRUE.equals(v.getSanctioned())
                ? commonLabels.amlReportSanctionedYes(locale)
                : commonLabels.amlReportSanctionedNo(locale);
        sb.append(commonLabels.amlReportSanctioned(locale, sanctionedVal)).append("\n");

        appendPaymentSummary(sb, v, locale);

        if (v.getResult() != null) {
            try {
                BitokResultResponse result = GSON.fromJson(v.getResult(), BitokResultResponse.class);

                if (result.getExposure() != null && !result.getExposure().isEmpty()) {
                    sb.append("\n").append(commonLabels.amlReportFundExposure(locale)).append("\n");
                    sb.append(commonLabels.amlReportExposureSource(locale)).append("\n");
                    for (BitokResultResponse.Exposure exp : result.getExposure()) {
                        String cat = exp.getEntityCategory() != null ? exp.getEntityCategory() : "Unknown";
                        String catName = commonLabels.amlCategoryName(locale, cat);
                        String share = exp.getValueShare() != null ? String.format("%.2f%%", exp.getValueShare() * 100) : "N/A";
                        sb.append("• ").append(categoryIcon(cat)).append(" ").append(catName).append(": ").append(share).append("\n");
                    }
                }

                if (result.getRisks() != null && !result.getRisks().isEmpty()) {
                    sb.append("\n").append(commonLabels.amlReportRisksHeader(locale)).append("\n");
                    for (BitokResultResponse.Risk risk : result.getRisks()) {
                        String cat = risk.getEntityCategory() != null ? risk.getEntityCategory() : "Unknown";
                        String catName = commonLabels.amlCategoryName(locale, cat);
                        String proximity = commonLabels.amlProximity(locale, risk.getProximity());
                        String share = risk.getValueShare() != null ? " (" + String.format("%.2f%%", risk.getValueShare() * 100) + ")" : "";
                        sb.append("• ").append(catName).append(": ").append(proximity).append(share).append("\n");
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (v.getCompletedAt() != null) {
            sb.append("\n").append(commonLabels.amlReportComputed(locale, formatDateToUtc(v.getCompletedAt()))).append("\n");
        }

        return sb.toString();
    }

    public String formatEllipticAmlReport(AmlVerification v, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(commonLabels.amlReportHeader(locale)).append("\n");
        sb.append(commonLabels.amlReportAddress(locale)).append(" `").append(v.getWalletAddress()).append("`\n\n");

        sb.append(commonLabels.amlReportRiskSummary(locale)).append("\n");
        String riskScore = v.getRiskScore() != null ? String.format("%.2f%%", v.getRiskScore() * 10) : "N/A";
        sb.append(commonLabels.amlReportRiskScore(locale, riskScore)).append("\n");

        String riskEmoji = riskLevelEmoji(v.getRiskLevel());
        String riskLevelName = commonLabels.amlRiskLevelName(locale, v.getRiskLevel());
        sb.append(commonLabels.amlReportRiskLevel(locale, riskEmoji + " " + riskLevelName)).append("\n");

        String sanctionedVal = Boolean.TRUE.equals(v.getSanctioned())
                ? commonLabels.amlReportSanctionedYes(locale)
                : commonLabels.amlReportSanctionedNo(locale);
        sb.append(commonLabels.amlReportSanctioned(locale, sanctionedVal)).append("\n");

        appendPaymentSummary(sb, v, locale);

        if (v.getResult() != null) {
            try {
                EllipticResultResponse result = GSON.fromJson(v.getResult(), EllipticResultResponse.class);
                EllipticResultResponse.EvaluationDetail evalDetail = result.getEvaluationDetail();

                logger.info("{}", evalDetail.getSource());
                if (evalDetail != null && evalDetail.getSource() != null && !evalDetail.getSource().isEmpty()) {
                    sb.append("\n").append(commonLabels.amlReportEllipticRulesHeader(locale)).append("\n");
                    for (EllipticResultResponse.SourceRule rule : evalDetail.getSource()) {
                        String ruleName = rule.getRuleName() != null ? rule.getRuleName() : "Unknown";
                        sb.append("• *").append(ruleName).append("* ").append("\n");

/*
                        if (rule.getMatchedElements() != null) {
                            for (EllipticResultResponse.MatchedElement element : rule.getMatchedElements()) {
                                String cat = element.getCategory() != null ? element.getCategory() : "Unknown";
                                String catIcon = categoryIcon(cat);
                                sb.append("  ").append(catIcon).append(" ").append(cat);
                                if (element.getContributions() != null) {
                                    for (EllipticResultResponse.Contribution contribution : element.getContributions()) {
                                        if (contribution.getEntity() != null) {
                                            String contribPct = contribution.getContributionPercentage() != null
                                                    ? String.format(" %.2f%%", contribution.getContributionPercentage())
                                                    : "";
                                            sb.append(": ").append(contribution.getEntity()).append(contribPct);
                                        }
                                    }
                                }
                                sb.append("\n");
                            }
                        }
*/
                    }
                }
            } catch (Exception e) {
                logger.error("exception when formating eliptic", e);
            }
        }

        if (v.getCompletedAt() != null) {
            sb.append("\n").append(commonLabels.amlReportComputed(locale, formatDateToUtc(v.getCompletedAt()))).append("\n");
        }

        return sb.toString();
    }

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

    public String formatReferral(ReferralDto referralDto) {
        if (referralDto == null) {
            return "-";
        }
        if (BalanceType.INDIVIDUAL.equals(referralDto.getType())) {
            String login = referralDto.getLogin() != null ? commonLabels.userLogin(referralDto.getLogin()) : "";
            String name = referralDto.getName() != null ? commonLabels.userName(referralDto.getName()) : "";
            return List.of(login, name).stream().filter(s -> !s.isEmpty()).collect(Collectors.joining(", "));
        } else if (BalanceType.GROUP.equals(referralDto.getType())) {
            return commonLabels.groupLabel(referralDto.getGroupName());
        }
        return "-";
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

    public static String formatDtUtc(Instant dt) {
        if (dt == null) {
            return "-";
        }
        return utcFormatter.format(dt) + " UTC";
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

    public static String formatUsdt(Long usdt) {
        return df.format(BigDecimal.valueOf(usdt).divide(BigDecimal.valueOf(1_000_000D)).setScale(2, RoundingMode.DOWN));
    }

    private void appendPaymentSummary(StringBuilder sb, AmlVerification v, Locale locale) {
        if (v.getPaidSun() == null) return;
        Long beforeSun = v.getBalanceBeforeSun();
        Long spentSun = v.getPaidSun();
        sb.append("\n").append(commonLabels.amlReportPaymentSummary(locale)).append("\n");
        sb.append(commonLabels.amlReportSpent(locale, formatBalance(spentSun) + " TRX")).append("\n");
        if (beforeSun != null) {
            sb.append(commonLabels.amlReportBalanceChange(locale,
                    formatBalance(beforeSun) + " TRX",
                    formatBalance(beforeSun - spentSun) + " TRX")).append("\n");
        }
    }

    public static String formatBalance(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D)).setScale(2, RoundingMode.DOWN));
    }

    public static String formatBalance3(Long balanceSun) {
        return df.format(BigDecimal.valueOf(balanceSun).divide(BigDecimal.valueOf(1_000_000D), 3, RoundingMode.DOWN));
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
