package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DepositData {
    private String coin;
    private String chain;
    private String amount;
    private String txID;
    private Integer status;
    private String confirmations;
    private String fromAddress;

    public Integer getConfirmationsInt() {
        try {
            return Integer.parseInt(confirmations);
        } catch (Exception e) {
            logger.error("Error parsing confirmations: {}", confirmations);
        }
        return 0;
    }
}
