package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class QuoteCheck {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private QuoteCheckResultOuter result;
}
