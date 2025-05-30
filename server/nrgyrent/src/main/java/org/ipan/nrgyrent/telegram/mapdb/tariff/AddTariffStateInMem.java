package org.ipan.nrgyrent.telegram.mapdb.tariff;

import org.ipan.nrgyrent.telegram.state.tariff.AddTariffState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class AddTariffStateInMem implements AddTariffState {
    String label;
    Long txType1Amount;

    public static AddTariffStateInMem of(AddTariffState prototype) {
        return AddTariffStateInMem.builder()
                .label(prototype.getLabel())
                .txType1Amount(prototype.getTxType1Amount())
                .build();
    }
}
