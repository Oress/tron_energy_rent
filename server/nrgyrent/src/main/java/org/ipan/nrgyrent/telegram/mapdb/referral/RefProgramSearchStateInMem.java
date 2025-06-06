package org.ipan.nrgyrent.telegram.mapdb.referral;

import org.ipan.nrgyrent.telegram.state.referral.RefProgramSearchState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class RefProgramSearchStateInMem implements RefProgramSearchState {
    Integer currentPage;
    String query;

    public static RefProgramSearchStateInMem of(RefProgramSearchState prototype) {
        return RefProgramSearchStateInMem.builder()
                .currentPage(prototype.getCurrentPage())
                .query(prototype.getQuery())
                .build();
    }
}
