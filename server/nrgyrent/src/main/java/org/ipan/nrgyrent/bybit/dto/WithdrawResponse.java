package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class WithdrawResponse {
    private Integer retCode;
    private String retMsg;
    private Result result;

    @Data
    public static class Result {
        private String id;
    }
}
