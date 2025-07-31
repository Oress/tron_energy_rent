package org.ipan.nrgyrent.domain.model.projections;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ipan.nrgyrent.domain.model.BalanceType;

@Data
@AllArgsConstructor
public class ReferralDto {
    private BalanceType type;

    private Long id;
    private String login;
    private String name;

    private String groupName;
}
