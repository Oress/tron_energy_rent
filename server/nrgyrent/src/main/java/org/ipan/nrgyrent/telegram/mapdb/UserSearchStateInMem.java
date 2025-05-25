package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.UserSearchState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class UserSearchStateInMem implements UserSearchState {
    Integer currentPage;
    String query;

    public static UserSearchStateInMem of(UserSearchState prototype) {
        return UserSearchStateInMem.builder()
                .currentPage(prototype.getCurrentPage())
                .query(prototype.getQuery())
                .build();
    }
}
