package org.ipan.nrgyrent.telegram.mapdb;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.UserState;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

@Value
@With
@Builder
public class UserStateInMem implements UserState {
    Long telegramId;
    States state;
    Long chatId;
    Integer menuMessageId;

    public static UserStateInMem of(UserState prototype) {
        return UserStateInMem.builder()
                .telegramId(prototype.getTelegramId())
                .state(prototype.getState())
                .chatId(prototype.getChatId())
                .menuMessageId(prototype.getMenuMessageId())
                .build();
    }

    public static class SerializerImpl implements Serializer<UserStateInMem> {
        private static final int version = 1;

        public static final int FIELD_TG = 0b00000001;
        public static final int FIELD_STATE = 0b00000010;
        public static final int FIELD_CHAT_ID = 0b00000100;
        public static final int FIELD_MENU_MSG_ID = 0b00001000;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull UserStateInMem value) throws IOException {
            int fields = 0;

            fields = value.telegramId != null ? (fields | FIELD_TG) : fields;
            fields = value.state != null ? (fields | FIELD_STATE) : fields;
            fields = value.chatId != null ? (fields | FIELD_CHAT_ID) : fields;
            fields = value.menuMessageId != null ? (fields | FIELD_MENU_MSG_ID) : fields;

            out.writeByte(version);
            out.writeInt(fields);

            if (value.telegramId != null) {
                out.writeLong(value.telegramId);
            }
            if (value.state != null) {
                out.writeUTF(value.state.name());
            }
            if (value.chatId != null) {
                out.writeLong(value.chatId);
            }
            if (value.menuMessageId != null) {
                out.writeInt(value.menuMessageId);
            }
        }

        @Override
        public UserStateInMem deserialize(@NotNull DataInput2 input, int available) throws IOException {
            int version = input.readByte();
            int fields = input.readInt();

            UserStateInMemBuilder builder = UserStateInMem.builder();
            if ((fields & FIELD_TG) != 0) {
                builder.telegramId(input.readLong());
            }
            if ((fields & FIELD_STATE) != 0) {
                builder.state(States.valueOf(input.readUTF()));
            }
            if ((fields & FIELD_CHAT_ID) != 0) {
                builder.chatId(input.readLong());
            }
            if ((fields & FIELD_MENU_MSG_ID) != 0) {
                builder.menuMessageId(input.readInt());
            }

            return builder.build();
        }
    }
}
