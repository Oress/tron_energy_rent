package org.ipan.nrgyrent.domain.model.projections;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletWithAutoTopupSession {
    String walletAddress;
    String walletLabel;
    Long activeSessionId;
}
