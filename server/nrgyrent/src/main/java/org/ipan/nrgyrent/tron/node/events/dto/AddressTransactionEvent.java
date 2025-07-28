package org.ipan.nrgyrent.tron.node.events.dto;

import lombok.Data;

@Data
public class AddressTransactionEvent {
    private Long timeStamp;
    private Long blockNumber;

    private String contractResult;

    private String assetName;
    private Long assetAmount;

    private String triggerName;
    private String transactionId;
    private String result;
    private String contractType;

    private String fromAddress;
    private String toAddress;
}
