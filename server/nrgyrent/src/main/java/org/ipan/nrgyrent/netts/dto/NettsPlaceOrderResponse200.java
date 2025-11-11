package org.ipan.nrgyrent.netts.dto;

import lombok.Data;

@Data
public class NettsPlaceOrderResponse200 {
    private DetailResponse detail;

    @Data
    public static class DetailResponse {
        private int code;
        private String msg;
        private DataResponse data;
    }

    @Data
    public static class DataResponse {
        private String orderId;
        private Double paidTRX;
        private String hash;
        private String delegateAddress;
        private int energy;
    }
}

/*
{
    "detail": {
        "code": 10000,
        "msg": "Successful, 2.23 TRX deducted",
        "data": {
            "orderId": "1H123456",
            "paidTRX": 2.23,
            "hash": "a1b2c3d4e5f6789...",
            "delegateAddress": "TDelegatePoolAddress...",
            "energy": 131050
        }
    }
}
*/