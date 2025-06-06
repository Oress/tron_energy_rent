package org.ipan.nrgyrent.telegram.state.referral;

public interface AddRefProgramState {
    String getLabel();
    Long getPercentage();

    AddRefProgramState withLabel(String value);
    AddRefProgramState withPercentage(Long value);
}
