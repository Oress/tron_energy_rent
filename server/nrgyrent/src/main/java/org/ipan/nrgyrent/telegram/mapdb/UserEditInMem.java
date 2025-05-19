package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;

import org.ipan.nrgyrent.telegram.state.UserEdit;
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
public class UserEditInMem implements UserEdit {
    Long selectedUserId;

    public static UserEditInMem of(UserEdit prototype) {
        return UserEditInMem.builder()
                .selectedUserId(prototype.getSelectedUserId())
                .build();
    }

    public static class SerializerImpl implements Serializer<UserEditInMem> {
        private static final int version = 1;

        public static final int FIELD_USER_ID = 0b00000001;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull UserEditInMem value) throws IOException {
            int fields = 0;

            fields = value.selectedUserId != null ? (fields | FIELD_USER_ID) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.selectedUserId != null) {
                out.writeLong(value.selectedUserId);
            }
        }

        @Override
        public UserEditInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            Long selectedUserId = null;

            if ((fields & FIELD_USER_ID) != 0) {
                selectedUserId = input.readLong();
            }

            return UserEditInMem.builder()
                    .selectedUserId(selectedUserId)
                    .build();
        }
    }
}
