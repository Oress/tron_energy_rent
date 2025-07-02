package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class QuoteApply {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private QuoteApplyResult result;
}
