package org.ipan.nrgyrent.telegram.mapdb;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.ipan.nrgyrent.telegram.state.DepositSearchState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramSearchState;

@Value
@With
@Builder
@Jacksonized
public class DepositSearchStateInMem implements DepositSearchState {
    Integer currentPage;

    public static DepositSearchStateInMem of(DepositSearchState prototype) {
        return DepositSearchStateInMem.builder()
                .currentPage(prototype.getCurrentPage())
                .build();
    }
}
