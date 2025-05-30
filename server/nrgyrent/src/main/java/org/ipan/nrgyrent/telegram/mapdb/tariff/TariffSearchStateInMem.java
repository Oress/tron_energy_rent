package org.ipan.nrgyrent.telegram.mapdb.tariff;

import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class TariffSearchStateInMem implements TariffSearchState {
    Integer currentPage;
    String query;

    public static TariffSearchStateInMem of(TariffSearchState prototype) {
        return TariffSearchStateInMem.builder()
                .currentPage(prototype.getCurrentPage())
                .query(prototype.getQuery())
                .build();
    }
}
