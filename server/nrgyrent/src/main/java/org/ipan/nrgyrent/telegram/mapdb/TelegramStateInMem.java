package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

@Service
public class TelegramStateInMem implements TelegramState {
    private final HTreeMap<Long, UserStateInMem> userStateMap;
    private final HTreeMap<Long, AddGroupStateInMem> addGroupStateMap;
    private final HTreeMap<Long, BalanceEditInMem> balanceEditMap;

    public TelegramStateInMem(DB db) {
        this.userStateMap = db.hashMap("userState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new UserStateInMem.SerializerImpl())
                .createOrOpen();

        this.addGroupStateMap = db.hashMap("addGroupState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new AddGroupStateInMem.SerializerImpl())
                .createOrOpen();

        this.balanceEditMap = db.hashMap("balanceEdit")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new BalanceEditInMem.SerializerImpl())
                .createOrOpen();
    }

    @Override
    public UserState getOrCreateUserState(Long userId) {
        return this.userStateMap.computeIfAbsent(userId, key -> new UserStateInMem(userId, States.START, null, null, null, null));
    }

    @Override
    public UserState updateUserState(Long userId, UserState userState) {
        return this.userStateMap.put(userId, UserStateInMem.of(userState));
    }

    @Override
    public AddGroupState getOrCreateAddGroupState(Long userId) {
        return this.addGroupStateMap.computeIfAbsent(userId, key -> new AddGroupStateInMem(null));
    }

    @Override
    public AddGroupState updateAddGroupState(Long userId, AddGroupState addGroupState) {
        return this.addGroupStateMap.put(userId, AddGroupStateInMem.of(addGroupState));
    }

    @Override
    public AddGroupState removeAddGroupState(Long userId) {
        return this.addGroupStateMap.remove(userId);
    }

    @Override
    public BalanceEdit getOrCreateBalanceEdit(Long userId) {
        return this.balanceEditMap.computeIfAbsent(userId, key -> new BalanceEditInMem(null));
    }

    @Override
    public BalanceEdit updateBalanceEdit(Long userId, BalanceEdit balanceEdit) {
        return this.balanceEditMap.put(userId, BalanceEditInMem.of(balanceEdit));
    }

    @Override
    public BalanceEdit removeBalanceEdit(Long userId) {
        return this.balanceEditMap.remove(userId);
    }
}
