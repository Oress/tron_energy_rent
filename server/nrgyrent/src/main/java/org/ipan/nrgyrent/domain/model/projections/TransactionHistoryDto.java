package org.ipan.nrgyrent.domain.model.projections;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TransactionHistoryDto {
    private String type;
    private Long id;
    private String correlationId;
    private String orderStatus;
    private String receiveAddress;
    private String fromAddress;
    private String withdrawalStatus;
    private Long amount;
    private Timestamp createdAt;
    private String balanceType;
}
