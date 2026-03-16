package org.ipan.nrgyrent.netts.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class BitokResultResponse {
    @SerializedName("risk_score")
    private Double riskScore;

    @SerializedName("risk_level")
    private String riskLevel;

    @SerializedName("entity_name")
    private String entityName;

    @SerializedName("entity_category")
    private String entityCategory;

    @SerializedName("exposure")
    private List<Exposure> exposure;

    @SerializedName("risks")
    private List<Risk> risks;

    @Data
    public static class Exposure {
        @SerializedName("value_share")
        private Double valueShare;

        @SerializedName("entity_category")
        private String entityCategory;
    }

    @Data
    public static class Risk {
        @SerializedName("rule")
        private Rule rule;

        @SerializedName("proximity")
        private String proximity;

        @SerializedName("risk_type")
        private String riskType;

        @SerializedName("risk_level")
        private String riskLevel;

        @SerializedName("detected_at")
        private String detectedAt;

        @SerializedName("occurred_at")
        private String occurredAt;

        @SerializedName("value_share")
        private Double valueShare;

        @SerializedName("fiat_currency")
        private String fiatCurrency;

        @SerializedName("value_in_fiat")
        private Double valueInFiat;

        @SerializedName("entity_category")
        private String entityCategory;
    }

    @Data
    public static class Rule {
        @SerializedName("rule_type")
        private String ruleType;

        @SerializedName("rule_sub_type")
        private String ruleSubType;

        @SerializedName("entity_category")
        private String entityCategory;

        @SerializedName("min_value_share")
        private Double minValueShare;

        @SerializedName("min_value_in_fiat")
        private Double minValueInFiat;
    }

    @Data
    public static class AddressUser {
        @SerializedName("sources")
        private List<String> sources;

        @SerializedName("user_id")
        private Long userId;
    }
}
