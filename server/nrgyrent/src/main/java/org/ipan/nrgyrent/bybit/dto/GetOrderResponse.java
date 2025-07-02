package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class GetOrderResponse {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private GetOrderResponseInner result;
}

