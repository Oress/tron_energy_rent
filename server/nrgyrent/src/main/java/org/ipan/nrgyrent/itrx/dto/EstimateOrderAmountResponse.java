package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class EstimateOrderAmountResponse {
    String period;
    String energy_amount;
    Integer price;
    Long total_price;
    Integer addition;
}
