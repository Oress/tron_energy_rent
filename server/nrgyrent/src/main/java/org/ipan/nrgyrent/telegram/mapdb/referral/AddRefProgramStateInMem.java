package org.ipan.nrgyrent.telegram.mapdb.referral;

import org.ipan.nrgyrent.telegram.state.referral.AddRefProgramState;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class AddRefProgramStateInMem implements AddRefProgramState {
    String label;
    Long percentage;

    public static AddRefProgramStateInMem of(AddRefProgramState prototype) {
        return AddRefProgramStateInMem.builder()
                .label(prototype.getLabel())
                .percentage(prototype.getPercentage())
                .build();
    }
}
