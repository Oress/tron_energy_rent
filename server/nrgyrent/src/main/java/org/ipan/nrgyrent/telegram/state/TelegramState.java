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

    UserEdit getOrCreateUserEdit(Long userId);
    UserEdit updateUserEdit(Long userId, UserEdit userEdit);
    UserEdit removeUserEdit(Long userId); // TODO: cleanup after finishing workflow

    TransactionParams getOrCreateTransactionParams(Long userId);
    TransactionParams updateTransactionParams(Long userId, TransactionParams transactionParams);
    TransactionParams removeTransactionParams(Long userId); // TODO: cleanup after finishing workflow

    WithdrawParams getOrCreateWithdrawParams(Long userId);
    WithdrawParams updateWithdrawParams(Long userId, WithdrawParams withdrawParams);
    WithdrawParams removeWithdrawParams(Long userId); // TODO: cleanup after finishing workflow
}
