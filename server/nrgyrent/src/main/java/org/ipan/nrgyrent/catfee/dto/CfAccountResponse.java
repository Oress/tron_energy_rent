package org.ipan.nrgyrent.catfee.dto;

import lombok.Data;

@Data
public class CfAccountResponse {
    // TRX account balance in SUN, as returned by GET /v1/account.
    private Long balance;
}
