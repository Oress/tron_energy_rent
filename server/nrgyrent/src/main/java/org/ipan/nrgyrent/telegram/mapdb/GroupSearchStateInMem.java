package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.GroupSearchState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class GroupSearchStateInMem implements GroupSearchState {
    Integer currentPage;
    String query;

    public static GroupSearchStateInMem of(GroupSearchState prototype) {
        return GroupSearchStateInMem.builder()
                .currentPage(prototype.getCurrentPage())
                .query(prototype.getQuery())
                .build();
    }
}
