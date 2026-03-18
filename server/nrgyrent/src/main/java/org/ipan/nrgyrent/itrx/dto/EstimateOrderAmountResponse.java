package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class EstimateOrderAmountResponse {
    Integer energy_amount;
    Long total_price;
}
