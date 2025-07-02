package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class QuoteCheckResult {
    private String accountType;
    private String exchangeTxId;
    private String userId;
    private String fromCoin;
    private String fromCoinType;
    private String fromAmount;
    private String toCoin;
    private String toCoinType;
    private String toAmount;
    private String exchangeStatus;
    private String convertRate;
    private String createdAt;
}
