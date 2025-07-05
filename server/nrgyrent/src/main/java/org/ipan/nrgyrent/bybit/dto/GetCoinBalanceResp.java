package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class GetCoinBalanceResp {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private GetCoinBalanceData result;
}
