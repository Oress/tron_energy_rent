package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class PlaceOrderResponse {
    private Integer errno; // 0 for success
    private String serial; // serial number of the order
    private Long amount; // amount of TRX in sun
    private Long balance;
    private String detail; // error message
}
