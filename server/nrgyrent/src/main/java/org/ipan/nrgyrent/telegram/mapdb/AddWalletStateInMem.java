package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.telegram.state.AddWalletState;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

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
