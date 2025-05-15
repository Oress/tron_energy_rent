package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
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
public class AddGroupStateInMem implements AddGroupState {
    String label;

    public static AddGroupStateInMem of(AddGroupState prototype) {
        return AddGroupStateInMem.builder()
                .label(prototype.getLabel())
                .build();
    }

    public static class SerializerImpl implements Serializer<AddGroupStateInMem> {
        private static final int version = 1;

        public static final int FIELD_LABEL = 0b00000001;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull AddGroupStateInMem value) throws IOException {
            int fields = 0;

            fields = value.label != null ? (fields | FIELD_LABEL) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.label != null) {
                out.writeUTF(value.label);
            }
        }

        @Override
        public AddGroupStateInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            AddGroupStateInMemBuilder builder = AddGroupStateInMem.builder();
            if ((fields & FIELD_LABEL) != 0) {
                builder.label(input.readUTF());
            }
            return builder.build();
        }
    }
}
