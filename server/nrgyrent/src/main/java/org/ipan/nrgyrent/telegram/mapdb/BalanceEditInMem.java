package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.telegram.state.BalanceEdit;
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
public class BalanceEditInMem implements BalanceEdit {
    Long selectedBalanceId;

    public static BalanceEditInMem of(BalanceEdit prototype) {
        return BalanceEditInMem.builder()
                .selectedBalanceId(prototype.getSelectedBalanceId())
                .build();
    }
}
