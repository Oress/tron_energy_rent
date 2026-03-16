package org.ipan.nrgyrent.netts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NettsAmlCreateRequest {
    private String address;
    private String network;
    private String provider;
    private Boolean wait;
}
