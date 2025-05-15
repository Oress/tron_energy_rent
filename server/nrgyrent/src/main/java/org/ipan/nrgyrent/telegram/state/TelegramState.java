package org.ipan.nrgyrent.telegram.state;

public interface TelegramState {
    UserState getOrCreateUserState(Long userId);
    UserState updateUserState(Long userId, UserState userState);

    AddGroupState getOrCreateAddGroupState(Long userId);
    AddGroupState removeAddGroupState(Long userId); // TODO: cleanup after finishing workflow
    AddGroupState updateAddGroupState(Long userId, AddGroupState addGroupState);

    BalanceEdit getOrCreateBalanceEdit(Long userId);
    BalanceEdit updateBalanceEdit(Long userId, BalanceEdit balanceEdit);
    BalanceEdit removeBalanceEdit(Long userId); // TODO: cleanup after finishing workflow
}
