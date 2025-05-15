package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
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
public class BalanceEditInMem implements BalanceEdit {
    Long selectedBalanceId;

    public static BalanceEditInMem of(BalanceEdit prototype) {
        return BalanceEditInMem.builder()
                .selectedBalanceId(prototype.getSelectedBalanceId())
                .build();
    }

    public static class SerializerImpl implements Serializer<BalanceEditInMem> {
        private static final int version = 1;

        public static final int FIELD_BALANCE_ID = 0b00000001;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull BalanceEditInMem value) throws IOException {
            int fields = 0;

            fields = value.selectedBalanceId != null ? (fields | FIELD_BALANCE_ID) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.selectedBalanceId != null) {
                out.writeLong(value.selectedBalanceId);
            }
        }

        @Override
        public BalanceEditInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            Long selectedBalanceId = null;

            if ((fields & FIELD_BALANCE_ID) != 0) {
                selectedBalanceId = input.readLong();
            }

            return BalanceEditInMem.builder()
                    .selectedBalanceId(selectedBalanceId)
                    .build();
        }
    }
}
