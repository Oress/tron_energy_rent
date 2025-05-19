package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.telegram.state.AddWalletState;
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
public class AddWalletStateInMem implements AddWalletState {
    String address;

    public static AddWalletStateInMem of(AddWalletState prototype) {
        return AddWalletStateInMem.builder()
                .address(prototype.getAddress())
                .build();
    }

    public static class SerializerImpl implements Serializer<AddWalletStateInMem> {
        private static final int version = 1;

        public static final int FIELD_ADDRESS = 0b00000001;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull AddWalletStateInMem value) throws IOException {
            int fields = 0;

            fields = value.address != null ? (fields | FIELD_ADDRESS) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.address != null) {
                out.writeUTF(value.address);
            }
        }

        @Override
        public AddWalletStateInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            AddWalletStateInMemBuilder builder = AddWalletStateInMem.builder();
            if ((fields & FIELD_ADDRESS) != 0) {
                builder.address(input.readUTF());
            }
            return builder.build();
        }
    }
}
