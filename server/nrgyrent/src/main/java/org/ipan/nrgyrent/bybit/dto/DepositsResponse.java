package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class DepositsResponse {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private DepositInner result;
}
