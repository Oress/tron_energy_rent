package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.WithdrawParams;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class WithdrawParamsInMem implements WithdrawParams {
    Long amount;
    Boolean groupBalance;

    public static WithdrawParamsInMem of(WithdrawParams prototype) {
        return WithdrawParamsInMem.builder()
                .amount(prototype.getAmount())
                .groupBalance(prototype.getGroupBalance())
                .build();
    }
}
