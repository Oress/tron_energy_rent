package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class QuoteConfirm {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private QuoteConfirmResult result;
}
