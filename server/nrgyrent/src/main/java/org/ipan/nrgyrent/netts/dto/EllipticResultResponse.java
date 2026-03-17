package org.ipan.nrgyrent.netts.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class EllipticResultResponse {
    @SerializedName("risk_score")
    private Double riskScore;

    @SerializedName("risk_score_detail")
    private RiskScoreDetail riskScoreDetail;

    @SerializedName("evaluation_detail")
    private EvaluationDetail evaluationDetail;

    @Data
    public static class RiskScoreDetail {
        @SerializedName("source")
        private Double source;

        @SerializedName("destination")
        private Double destination;
    }

    @Data
    public static class EvaluationDetail {
        @SerializedName("source")
        private List<SourceRule> source;
    }

    @Data
    public static class SourceRule {
        @SerializedName("rule_id")
        private String ruleId;

        @SerializedName("rule_name")
        private String ruleName;

        @SerializedName("rule_type")
        private String ruleType;

        @SerializedName("risk_score")
        private Double riskScore;

/*        @SerializedName("matched_elements")
        private List<MatchedElement> matchedElements;*/
    }

    @Data
    public static class MatchedElement {
        @SerializedName("category")
        private String category;

        @SerializedName("category_id")
        private String categoryId;

        @SerializedName("contribution_percentage")
        private Double contributionPercentage;

        @SerializedName("contributions")
        private List<Contribution> contributions;
    }

    @Data
    public static class Contribution {
        @SerializedName("entity")
        private String entity;

        @SerializedName("contribution_percentage")
        private Double contributionPercentage;
    }
}
