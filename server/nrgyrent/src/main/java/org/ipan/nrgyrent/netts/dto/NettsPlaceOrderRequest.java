package org.ipan.nrgyrent.netts.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NettsPlaceOrderRequest {
    private Integer amount;
    private String receiveAddress;
}
