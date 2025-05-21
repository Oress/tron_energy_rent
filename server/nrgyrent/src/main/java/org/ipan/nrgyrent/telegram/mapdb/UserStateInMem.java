package org.ipan.nrgyrent.telegram.mapdb;

import java.io.IOException;
import java.util.ArrayList;
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
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
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
}
