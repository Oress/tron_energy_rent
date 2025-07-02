package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class InternalTransferResponse {
    private Integer retCode;
    private String retMsg;
    private Long time;
    private InternalTransferData result;
}

