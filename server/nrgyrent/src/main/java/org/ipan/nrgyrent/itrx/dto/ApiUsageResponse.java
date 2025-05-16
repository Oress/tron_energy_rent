package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class ApiUsageResponse {
    private Long balance;
    private Integer total_count;
    private Long total_sum_energy;
    private Long total_sum_trx;
}
