package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.BalanceEdit;

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
