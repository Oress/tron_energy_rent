package org.ipan.nrgyrent.netts.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class NettsPricingResponse {
    private boolean success;
    private String version;
    private String timestamp;
    private DataResponse data;

    @Getter
    @ToString
    public static class DataResponse {
        @SerializedName("trx_rate_usd")
        private Double trxRateUsd;
        private ServicesResponse services;
    }

    @Getter
    @ToString
    public static class ServicesResponse {
        @SerializedName("energy_1h")
        private EnergyServiceResponse energy1h;
    }

    @Getter
    @ToString
    public static class EnergyServiceResponse {
        private String unit;
        @SerializedName("current_period")
        private String currentPeriod;
        private List<PeriodResponse> periods;
    }

    @Getter
    @ToString
    public static class PeriodResponse {
        private String id;
        private String label;
        private String start;
        private String end;
        @SerializedName("is_current")
        private Boolean isCurrent;
        private Long price;
    }
}
