package org.ipan.nrgyrent.telegram.mapdb.referral;

import org.ipan.nrgyrent.telegram.state.referral.RefProgramEdit;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class RefProgramEditInMem implements RefProgramEdit {
    Long selectedRefProgramId;

    public static RefProgramEditInMem of(RefProgramEdit prototype) {
        return RefProgramEditInMem.builder()
                .selectedRefProgramId(prototype.getSelectedRefProgramId())
                .build();
    }
}
