package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class TransactionParamsInMem implements TransactionParams {
    Integer energyAmount;
    Boolean groupBalance;

    public static TransactionParamsInMem of(TransactionParams prototype) {
        return TransactionParamsInMem.builder()
                .energyAmount(prototype.getEnergyAmount())
                .groupBalance(prototype.getGroupBalance())
                .build();
    }
}
