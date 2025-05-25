package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.AddWalletState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class AddWalletStateInMem implements AddWalletState {
    String address;

    public static AddWalletStateInMem of(AddWalletState prototype) {
        return AddWalletStateInMem.builder()
                .address(prototype.getAddress())
                .build();
    }
}
