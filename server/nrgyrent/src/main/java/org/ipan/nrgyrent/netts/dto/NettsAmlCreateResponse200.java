package org.ipan.nrgyrent.netts.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NettsAmlCreateResponse200 {
    private int code;
    private String msg;
    private NettsAmlCreateResponse200.DataResponse data;


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

        @SerializedName("price_usdt")
        private Double price_usdt;

        @SerializedName("price_trx")
        private Double price_trx;

        @SerializedName("message")
        private String message;
    }
}
