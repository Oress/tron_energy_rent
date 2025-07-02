package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

@Data
public class DepositData {
    private String coin;
    private String chain;
    private String amount;
    private String txID;
    private String status;
    private String confirmations;
    private String fromAddress;
}
