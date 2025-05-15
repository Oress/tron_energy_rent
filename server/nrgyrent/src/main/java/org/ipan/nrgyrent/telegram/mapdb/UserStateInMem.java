package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.telegram.States;
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
public class UserStateInMem implements UserState {
    Long telegramId;
    States state;
    Long chatId;
    Integer menuMessageId;
    UserRole role;
    List<Integer> messagesToDelete;

    public static UserStateInMem of(UserState prototype) {
        return UserStateInMem.builder()
                .telegramId(prototype.getTelegramId())
                .state(prototype.getState())
                .chatId(prototype.getChatId())
                .menuMessageId(prototype.getMenuMessageId())
                .role(prototype.getRole())
                .messagesToDelete(prototype.getMessagesToDelete() == null ? Collections.emptyList() : new ArrayList<>(prototype.getMessagesToDelete()))
                .build();
    }

    public static class SerializerImpl implements Serializer<UserStateInMem> {
        private static final int version = 1;

        public static final int FIELD_TG = 0b00000001;
        public static final int FIELD_STATE = FIELD_TG << 1;
        public static final int FIELD_CHAT_ID = FIELD_STATE << 1;
        public static final int FIELD_MENU_MSG_ID = FIELD_CHAT_ID << 1;
        public static final int FIELD_ROLE = FIELD_MENU_MSG_ID << 1;
        public static final int FIELD_MSGS_TO_DELETE = FIELD_ROLE << 1;

        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull UserStateInMem value) throws IOException {
            int fields = 0;

            fields = value.telegramId != null ? (fields | FIELD_TG) : fields;
            fields = value.state != null ? (fields | FIELD_STATE) : fields;
            fields = value.chatId != null ? (fields | FIELD_CHAT_ID) : fields;
            fields = value.menuMessageId != null ? (fields | FIELD_MENU_MSG_ID) : fields;
            fields = value.role != null ? (fields | FIELD_ROLE) : fields;
            fields = value.messagesToDelete != null ? (fields | FIELD_MSGS_TO_DELETE) : fields;

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
            if (value.role != null) {
                out.writeUTF(value.role.name());
            }
            if (value.messagesToDelete != null) {
                out.writeInt(value.messagesToDelete.size());
                for (Integer messageId : value.messagesToDelete) {
                    out.writeInt(messageId);
                }
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
                String utf = input.readUTF();
                builder.state(States.valueOf(utf));
            }
            if ((fields & FIELD_CHAT_ID) != 0) {
                builder.chatId(input.readLong());
            }
            if ((fields & FIELD_MENU_MSG_ID) != 0) {
                builder.menuMessageId(input.readInt());
            }
            if ((fields & FIELD_ROLE) != 0) {
                String utf = input.readUTF();
                builder.role(UserRole.valueOf(utf));
            }
            if ((fields & FIELD_MSGS_TO_DELETE) != 0) {
                int size = input.readInt();
                List<Integer> messagesToDelete = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    messagesToDelete.add(input.readInt());
                }
                builder.messagesToDelete(messagesToDelete);
            }
            return builder.build();
        }
    }
}
