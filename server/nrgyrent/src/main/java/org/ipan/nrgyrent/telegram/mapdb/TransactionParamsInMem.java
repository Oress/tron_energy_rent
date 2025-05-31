package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.state.TransactionParams;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class TransactionParamsInMem implements TransactionParams {
    Integer numberOfTransactions;
    Integer energyAmount;
    Boolean groupBalance;

    public static TransactionParamsInMem of(TransactionParams prototype) {
        return TransactionParamsInMem.builder()
                .energyAmount(prototype.getEnergyAmount())
                .groupBalance(prototype.getGroupBalance())
                .numberOfTransactions(prototype.getNumberOfTransactions())
                .build();
    }
}
