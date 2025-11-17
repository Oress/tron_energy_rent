package org.ipan.nrgyrent.tron.node.api.dto;

import lombok.Data;

@Data
public class AccountResource {
    private final Integer EnergyUsed;
    private final Integer EnergyLimit;
    private final Integer TotalNetLimit;
}
