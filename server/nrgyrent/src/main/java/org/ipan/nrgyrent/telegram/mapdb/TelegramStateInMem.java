package org.ipan.nrgyrent.telegram.mapdb;

import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.mapdb.tariff.TariffEditInMem;
import org.ipan.nrgyrent.telegram.mapdb.tariff.TariffSearchStateInMem;
import org.ipan.nrgyrent.telegram.state.AddWalletState;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.GroupSearchState;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.TransactionParams;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserSearchState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.WithdrawParams;
import org.ipan.nrgyrent.telegram.state.AddGroupState;
import org.ipan.nrgyrent.telegram.state.tariff.AddTariffState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffEdit;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;
import org.ipan.nrgyrent.telegram.mapdb.tariff.AddTariffStateInMem;
import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TelegramStateInMem implements TelegramState {
    private final HTreeMap<Long, UserStateInMem> userStateMap;
    private final HTreeMap<Long, AddGroupStateInMem> addGroupStateMap;
    private final HTreeMap<Long, BalanceEditInMem> balanceEditMap;
    private final HTreeMap<Long, AddWalletStateInMem> addWalletStateMap;
    private final HTreeMap<Long, TransactionParamsInMem> transactionParamsMap;
    private final HTreeMap<Long, UserEditInMem> userEditMap;
    private final HTreeMap<Long, WithdrawParamsInMem> withdrawParamsMap;
    private final HTreeMap<Long, GroupSearchStateInMem> groupSearchState;
    private final HTreeMap<Long, UserSearchStateInMem> userSearchState;
    private final HTreeMap<Long, TariffSearchStateInMem> tariffSearchState;
    private final HTreeMap<Long, TariffEditInMem> tariffEditState;
    private final HTreeMap<Long, AddTariffStateInMem> addTariffStateMap;

    public TelegramStateInMem(DB db, ObjectMapper objectMapper) {
        this.userStateMap = db.hashMap("userState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(UserStateInMem.class, objectMapper))
                .createOrOpen();

        this.addGroupStateMap = db.hashMap("addGroupState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(AddGroupStateInMem.class, objectMapper))
                .createOrOpen();

        this.balanceEditMap = db.hashMap("balanceEdit")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(BalanceEditInMem.class, objectMapper))
                .createOrOpen();

        this.addWalletStateMap = db.hashMap("addWalletState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(AddWalletStateInMem.class, objectMapper))
                .createOrOpen();

        this.transactionParamsMap = db.hashMap("transactionParams")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(TransactionParamsInMem.class, objectMapper))
                .createOrOpen();

        this.userEditMap = db.hashMap("userEdit")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(UserEditInMem.class, objectMapper))
                .createOrOpen();

        this.withdrawParamsMap = db.hashMap("withdrawParams")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(WithdrawParamsInMem.class, objectMapper))
                .createOrOpen();

        this.groupSearchState = db.hashMap("groupSearchState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(GroupSearchStateInMem.class, objectMapper))
                .createOrOpen();

        this.userSearchState = db.hashMap("userSearchState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(UserSearchStateInMem.class, objectMapper))
                .createOrOpen();

        this.tariffSearchState = db.hashMap("tariffSearchState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(TariffSearchStateInMem.class, objectMapper))
                .createOrOpen();
        this.tariffEditState = db.hashMap("tariffEditState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(TariffEditInMem.class, objectMapper))
                .createOrOpen();
        this.addTariffStateMap = db.hashMap("addTariffState")
                .keySerializer(Serializer.LONG)
                .valueSerializer(new GenericSerializer<>(AddTariffStateInMem.class, objectMapper))
                .createOrOpen();
    }

    @Override
    public UserState getOrCreateUserState(Long userId) {
        return this.userStateMap.computeIfAbsent(userId, key -> new UserStateInMem(userId, States.START, null, null, null, null, null));
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
        return this.addWalletStateMap.computeIfAbsent(userId, key -> AddWalletStateInMem.builder().build());
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
        return this.transactionParamsMap.computeIfAbsent(userId, key -> TransactionParamsInMem.builder().build());
    }

    @Override
    public TransactionParams updateTransactionParams(Long userId, TransactionParams transactionParams) {
        return this.transactionParamsMap.put(userId, TransactionParamsInMem.of(transactionParams));
    }

    @Override
    public TransactionParams removeTransactionParams(Long userId) {
        return this.transactionParamsMap.remove(userId);
    }

    @Override
    public UserEdit getOrCreateUserEdit(Long userId) {
        return this.userEditMap.computeIfAbsent(userId, key -> new UserEditInMem(null));
    }

    @Override
    public UserEdit updateUserEdit(Long userId, UserEdit userEdit) {
        return this.userEditMap.put(userId, UserEditInMem.of(userEdit));
    }

    @Override
    public UserEdit removeUserEdit(Long userId) {
        return this.userEditMap.remove(userId);
    }

    @Override
    public WithdrawParams getOrCreateWithdrawParams(Long userId) {
        return this.withdrawParamsMap.computeIfAbsent(userId, key -> new WithdrawParamsInMem(null, null));
    }

    @Override
    public WithdrawParams updateWithdrawParams(Long userId, WithdrawParams withdrawParams) {
        return this.withdrawParamsMap.put(userId, WithdrawParamsInMem.of(withdrawParams));
    }

    @Override
    public WithdrawParams removeWithdrawParams(Long userId) {
        return this.withdrawParamsMap.remove(userId);
    }

    @Override
    public GroupSearchState getOrCreateGroupSearchState(Long userId) {
        return this.groupSearchState.computeIfAbsent(userId, key -> new GroupSearchStateInMem(null,null));
    }

    @Override
    public GroupSearchState updateGroupSearchState(Long userId, GroupSearchState groupSearchState) {
        return this.groupSearchState.put(userId, GroupSearchStateInMem.of(groupSearchState));
    }

    @Override
    public GroupSearchState removeGroupSearchState(Long userId) {
        return this.groupSearchState.remove(userId);
    }

    @Override
    public UserSearchState getOrCreateUserSearchState(Long userId) {
        return this.userSearchState.computeIfAbsent(userId, key -> new UserSearchStateInMem(null,null));
    }

    @Override
    public UserSearchState updateUserSearchState(Long userId, UserSearchState groupSearchState) {
        return this.userSearchState.put(userId, UserSearchStateInMem.of(groupSearchState));
    }

    @Override
    public UserSearchState removeUserSearchState(Long userId) {
        return this.userSearchState.remove(userId);
    }

    @Override
    public TariffSearchState getOrCreateTariffSearchState(Long userId) {
        return this.tariffSearchState.computeIfAbsent(userId, key -> TariffSearchStateInMem.builder().build());
    }

    @Override
    public TariffSearchState updateTariffSearchState(Long userId, TariffSearchState groupSearchState) {
        return this.tariffSearchState.put(userId, TariffSearchStateInMem.of(groupSearchState));
    }

    @Override
    public TariffSearchState removeTariffSearchState(Long userId) {
        return this.tariffSearchState.remove(userId);
    }

    @Override
    public TariffEdit getOrCreateTariffEdit(Long userId) {
        return this.tariffEditState.computeIfAbsent(userId, key -> TariffEditInMem.builder().build());
    }

    @Override
    public TariffEdit updateTariffEdit(Long userId, TariffEdit tariffEdit) {
        return this.tariffEditState.put(userId, TariffEditInMem.of(tariffEdit));
    }

    @Override
    public TariffEdit removeTariffEdit(Long userId) {
        return this.tariffEditState.remove(userId);
    }

    @Override
    public AddTariffState getOrCreateAddTariffState(Long userId) {
        return this.addTariffStateMap.computeIfAbsent(userId, key -> AddTariffStateInMem.builder().build());
    }

    @Override
    public AddTariffState removeAddTariffState(Long userId) {
    return this.addTariffStateMap.remove(userId);
    }

    @Override
    public AddTariffState updateAddTariffState(Long userId, AddTariffState addTariffState) {
        return this.addTariffStateMap.put(userId, AddTariffStateInMem.of(addTariffState));
    }
}
