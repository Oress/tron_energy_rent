package org.ipan.nrgyrent.domain.model.repository;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class DepositHistoryItem {
    String txId;
    Long amountSun;
    Long amountUsdt;
    Long timestamp;
}
