package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetCoinBalanceData {
    private Balance balance;

    @Data
    public static class Balance {
        private BigDecimal walletBalance;
        private String coin;
    }
}