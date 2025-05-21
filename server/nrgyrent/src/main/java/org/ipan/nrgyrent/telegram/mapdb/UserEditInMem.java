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
import lombok.extern.jackson.Jacksonized;

@Value
@With
@Builder
@Jacksonized
public class UserEditInMem implements UserEdit {
    Long selectedUserId;

    public static UserEditInMem of(UserEdit prototype) {
        return UserEditInMem.builder()
                .selectedUserId(prototype.getSelectedUserId())
                .build();
    }
}
