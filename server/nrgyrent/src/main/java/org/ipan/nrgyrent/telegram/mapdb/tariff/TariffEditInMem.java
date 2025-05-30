package org.ipan.nrgyrent.telegram.mapdb.tariff;

import org.ipan.nrgyrent.telegram.state.tariff.TariffEdit;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class TariffEditInMem implements TariffEdit {
    Long selectedTariffId;

    public static TariffEditInMem of(TariffEdit prototype) {
        return TariffEditInMem.builder()
                .selectedTariffId(prototype.getSelectedTariffId())
                .build();
    }
}
