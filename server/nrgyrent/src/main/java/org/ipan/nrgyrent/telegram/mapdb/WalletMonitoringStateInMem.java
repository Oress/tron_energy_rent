package org.ipan.nrgyrent.telegram.mapdb;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.ipan.nrgyrent.telegram.state.WalletMonitoringState;

@Value
@With
@Builder
@Jacksonized
public class WalletMonitoringStateInMem implements WalletMonitoringState {
    String address;
    Long sessionId;

    public static WalletMonitoringStateInMem of(WalletMonitoringState prototype) {
        return WalletMonitoringStateInMem.builder()
                .address(prototype.getAddress())
                .sessionId(prototype.getSessionId())
                .build();
    }
}
