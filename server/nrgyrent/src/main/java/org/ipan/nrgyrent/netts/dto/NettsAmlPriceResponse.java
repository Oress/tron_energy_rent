package org.ipan.nrgyrent.netts.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NettsAmlPriceResponse {
    private boolean success;
    private DataResponse data;
    private String timestamp;

    @Getter
    @ToString
    public static class DataResponse {
        @SerializedName("provider")
        private String provider;

        @SerializedName("price_usdt")
        private Double priceUsdt;

        @SerializedName("price_trx")
        private Double priceTrx;

        @SerializedName("trx_rate_usd")
        private Double trxRateUsd;

        @SerializedName("available")
        private Boolean available;

        @SerializedName("user_balance_trx")
        private Double userBalanceTrx;

        @SerializedName("can_afford")
        private Boolean canAfford;
    }
}
