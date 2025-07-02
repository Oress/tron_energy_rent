package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class PlaceOrderResponse {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private PlaceOrderData result;
}

