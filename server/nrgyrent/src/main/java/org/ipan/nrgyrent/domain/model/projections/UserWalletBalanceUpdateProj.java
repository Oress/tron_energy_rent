package org.ipan.nrgyrent.domain.model.projections;

import lombok.Data;

@Data
public class UserWalletBalanceUpdateProj {
    Long userId;
    Long username;
    Long sunBalance;

    String depositAddress;
    String lastTxId;
    Long lastTxTimestamp;
}
