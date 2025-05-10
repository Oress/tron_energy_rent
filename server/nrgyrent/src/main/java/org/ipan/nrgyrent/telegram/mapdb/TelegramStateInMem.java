package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramState;
import org.ipan.nrgyrent.telegram.UserState;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

@Service
public class TelegramStateInMem implements TelegramState {
    private final HTreeMap<Long, UserStateInMem> userStateMap;

    public TelegramStateInMem(DB db) {
        this.userStateMap = db.hashMap("userState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new UserStateInMem.SerializerImpl())
                .createOrOpen();
    }

    @Override
    public UserState getOrCreateUserState(Long userId) {
        return this.userStateMap.computeIfAbsent(userId, key -> userStateMap.computeIfAbsent(userId, k -> new UserStateInMem(userId, States.START, null, null)));
    }

    @Override
    public UserState updateUserState(Long userId, UserState userState) {
        return this.userStateMap.put(userId, UserStateInMem.of(userState));
    }
}
