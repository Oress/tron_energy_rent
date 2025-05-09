package org.ipan.nrgyrent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Wallet {
    private Long id;
    private String walletAddress;
}
