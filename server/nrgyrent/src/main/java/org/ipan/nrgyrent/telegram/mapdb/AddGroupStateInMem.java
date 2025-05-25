package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.AddGroupState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class AddGroupStateInMem implements AddGroupState {
    String label;

    public static AddGroupStateInMem of(AddGroupState prototype) {
        return AddGroupStateInMem.builder()
                .label(prototype.getLabel())
                .build();
    }
}
