package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
import org.ipan.nrgyrent.telegram.state.AddWalletState;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
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
    private final HTreeMap<Long, AddWalletStateInMem> addWalletStateMap;
    private final HTreeMap<Long, TransactionParamsInMem> transactionParamsMap;

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

        this.addWalletStateMap = db.hashMap("addWalletState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new AddWalletStateInMem.SerializerImpl())
                .createOrOpen();

        this.transactionParamsMap = db.hashMap("transactionParams")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new TransactionParamsInMem.SerializerImpl())
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

    @Override
    public AddWalletState getOrCreateAddWalletState(Long userId) {
        return this.addWalletStateMap.computeIfAbsent(userId, key -> new AddWalletStateInMem(null));
    }

    @Override
    public AddWalletState removeAddWalletState(Long userId) {
        return this.addWalletStateMap.remove(userId);
    }

    @Override
    public AddWalletState updateAddWalletState(Long userId, AddWalletState addWalletState) {
        return this.addWalletStateMap.put(userId, AddWalletStateInMem.of(addWalletState));
    }

    @Override
    public TransactionParams getOrCreateTransactionParams(Long userId) {
        return this.transactionParamsMap.computeIfAbsent(userId, key -> new TransactionParamsInMem(null, null));
    }

    @Override
    public TransactionParams updateTransactionParams(Long userId, TransactionParams transactionParams) {
        return this.transactionParamsMap.put(userId, TransactionParamsInMem.of(transactionParams));
    }

    @Override
    public TransactionParams removeTransactionParams(Long userId) {
        return this.transactionParamsMap.remove(userId);
    }
}
