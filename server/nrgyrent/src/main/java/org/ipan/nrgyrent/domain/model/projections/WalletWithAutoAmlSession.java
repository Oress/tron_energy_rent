package org.ipan.nrgyrent.domain.model.projections;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletWithAutoAmlSession {
    String walletAddress;
    String walletLabel;
    Long activeSessionId;
    Long thresholdSun;
}

