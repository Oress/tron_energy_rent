package org.ipan.nrgyrent.telegram.state;

public interface TelegramState {
    UserState getOrCreateUserState(Long userId);
    UserState updateUserState(Long userId, UserState userState);

    AddGroupState getOrCreateAddGroupState(Long userId);
    AddGroupState removeAddGroupState(Long userId); // TODO: cleanup after finishing workflow
    AddGroupState updateAddGroupState(Long userId, AddGroupState addGroupState);

    AddWalletState getOrCreateAddWalletState(Long userId);
    AddWalletState removeAddWalletState(Long userId); // TODO: cleanup after finishing workflow
    AddWalletState updateAddWalletState(Long userId, AddWalletState addWalletState);

    BalanceEdit getOrCreateBalanceEdit(Long userId);
    BalanceEdit updateBalanceEdit(Long userId, BalanceEdit balanceEdit);
    BalanceEdit removeBalanceEdit(Long userId); // TODO: cleanup after finishing workflow
}
