package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class GetOrderData {
    private String symbol;
    private String orderType;
    private String orderId;
    private String cancelType;
    private String avgPrice;
    private String orderStatus;
    private String rejectReason;
    private String cumExecValue;
    private String cumExecFee;
    private String cumExecQty;
}
