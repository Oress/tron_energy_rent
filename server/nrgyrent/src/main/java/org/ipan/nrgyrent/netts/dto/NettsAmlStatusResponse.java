package org.ipan.nrgyrent.netts.dto;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class NettsAmlStatusResponse {
    private boolean success;
    private NettsAmlStatusResponse.DataResponse data;
    private String timestamp;

    @Data
    public static class DataResponse {
        @SerializedName("client_order_id")
        private String clientOrderId;

        @SerializedName("status")
        private String status;

        @SerializedName("address")
        private String address;

        @SerializedName("provider")
        private String provider;

        @SerializedName("report_language")
        private String reportLanguage;

        @SerializedName("risk_score")
        private Double riskScore;

        @SerializedName("risk_level")
        private String riskLevel;

        @SerializedName("is_sanctioned")
        private Boolean isSanctioned;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("completed_at")
        private String completedAt;

        @SerializedName("message")
        private String message;

        @SerializedName("result")
        private JsonObject result;
/*
        @SerializedName("address_users")
        private List<AddressUser> addressUsers;*/
    }
}


