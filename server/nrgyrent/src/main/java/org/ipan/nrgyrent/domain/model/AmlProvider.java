package org.ipan.nrgyrent.domain.model;

import java.util.List;

public enum AmlProvider {
    ELLIPTIC("ELLIPTIC"),
    BITOK("BITOK"),
    ELLIPTIC_AND_BITOK("ELLIPTIC & BITOK");

    private final String displayName;

    AmlProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves this provider selection into the concrete providers that must actually be queried.
     * {@link #ELLIPTIC_AND_BITOK} expands into both individual providers (yielding one report each),
     * while every other value maps to itself.
     */
    public List<AmlProvider> concreteProviders() {
        return this == ELLIPTIC_AND_BITOK
                ? List.of(ELLIPTIC, BITOK)
                : List.of(this);
    }
}
