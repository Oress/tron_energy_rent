package org.ipan.nrgyrent.domain.model;

public enum AmlVerificationStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    SKIPPED;

    public static AmlVerificationStatus fromString(String status) {
        for (AmlVerificationStatus value : AmlVerificationStatus.values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid AmlVerificationStatus: " + status);
    }
}
