package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class QuoteApplyResult {
    private String quoteTxId;
    private String exchangeRate;
    private String fromCoin;
    private String fromCoinType;
    private String toCoin;
    private String toCoinType;
    private String fromAmount;
    private String toAmount;
    private String expiredTime;
    private String requestId;
//    private Integer extTaxAndFee;
}
