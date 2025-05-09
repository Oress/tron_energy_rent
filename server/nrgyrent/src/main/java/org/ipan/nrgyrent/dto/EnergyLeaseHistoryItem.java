package org.ipan.nrgyrent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class EnergyLeaseHistoryItem {
    private Long id;
    private String walletAddressBase58;
    private BigDecimal amountTrx;
    private Integer durationHours;
    private Instant createdAt;
}
