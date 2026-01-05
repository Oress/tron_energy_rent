package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListDelegatePolicyResponse {
    private List<DelegatePolicyResponse> results;
}