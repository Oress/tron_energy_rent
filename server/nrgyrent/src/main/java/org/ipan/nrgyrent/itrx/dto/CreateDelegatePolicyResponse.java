package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class CreateDelegatePolicyResponse {
    private Integer errno; // 0 for success
    private Long balance;
    private String detail; // error message
}
