package org.ipan.nrgyrent.tron.node.api.dto;

import lombok.Data;

@Data
public class AccountResource {
    private final Long EnergyUsed;
    private final Long EnergyLimit;
    private final Long TotalNetLimit;
}
