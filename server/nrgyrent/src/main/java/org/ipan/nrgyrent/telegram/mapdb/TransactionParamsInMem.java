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

@Value
@With
@Builder
public class TransactionParamsInMem implements TransactionParams {
    Integer energyAmount;
    Boolean groupBalance;

    public static TransactionParamsInMem of(TransactionParams prototype) {
        return TransactionParamsInMem.builder()
                .energyAmount(prototype.getEnergyAmount())
                .groupBalance(prototype.getGroupBalance())
                .build();
    }

    public static class SerializerImpl implements Serializer<TransactionParamsInMem> {
        private static final int version = 1;

        public static final int FIELD_ENERGY_AMOUNT = 0b00000001;
        public static final int FIELD_USE_GROUP_BALANCE = 0b00000010;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull TransactionParamsInMem value) throws IOException {
            int fields = 0;

            fields = value.energyAmount != null ? (fields | FIELD_ENERGY_AMOUNT) : fields;
            fields = value.groupBalance != null ? (fields | FIELD_USE_GROUP_BALANCE) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.energyAmount != null) {
                out.writeInt(value.energyAmount);
            }
            if (value.groupBalance != null) {
                out.writeBoolean(value.groupBalance);
            }
        }

        @Override
        public TransactionParamsInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            Integer energyAmount = null;
            Boolean groupBalance = null;
            if ((fields & FIELD_ENERGY_AMOUNT) != 0) {
                energyAmount = input.readInt();
            }
            if ((fields & FIELD_USE_GROUP_BALANCE) != 0) {
                groupBalance = input.readBoolean();
            }
            return TransactionParamsInMem.builder()
                    .energyAmount(energyAmount)
                    .groupBalance(groupBalance)
                    .build();
        }
    }
}
