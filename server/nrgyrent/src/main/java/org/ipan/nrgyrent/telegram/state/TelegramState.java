package org.ipan.nrgyrent.telegram.state;

import org.ipan.nrgyrent.telegram.state.tariff.AddTariffState;
import org.ipan.nrgyrent.telegram.state.referral.AddRefProgramState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramEdit;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramSearchState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffEdit;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;

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

    GroupSearchState getOrCreateGroupSearchState(Long userId);
    GroupSearchState updateGroupSearchState(Long userId, GroupSearchState groupSearchState);
    GroupSearchState removeGroupSearchState(Long userId); // TODO: cleanup after finishing workflow

    UserSearchState getOrCreateUserSearchState(Long userId);
    UserSearchState updateUserSearchState(Long userId, UserSearchState groupSearchState);
    UserSearchState removeUserSearchState(Long userId); // TODO: cleanup after finishing workflow

    TariffSearchState getOrCreateTariffSearchState(Long userId);
    TariffSearchState updateTariffSearchState(Long userId, TariffSearchState groupSearchState);
    TariffSearchState removeTariffSearchState(Long userId); // TODO: cleanup after finishing workflow

    TariffEdit getOrCreateTariffEdit(Long userId);
    TariffEdit updateTariffEdit(Long userId, TariffEdit tariffEdit);
    TariffEdit removeTariffEdit(Long userId); // TODO: cleanup after finishing workflow

    AddTariffState getOrCreateAddTariffState(Long userId);
    AddTariffState removeAddTariffState(Long userId); // TODO: cleanup after finishing workflow
    AddTariffState updateAddTariffState(Long userId, AddTariffState addTariffState);

    RefProgramSearchState getOrCreateRefProgramSearchState(Long userId);
    RefProgramSearchState updateRefProgramSearchState(Long userId, RefProgramSearchState groupSearchState);

    RefProgramEdit getOrCreateRefProgramEdit(Long userId);
    RefProgramEdit updateRefProgramEdit(Long userId, RefProgramEdit tariffEdit);

    AddRefProgramState getOrCreateAddRefProgramState(Long userId);
    AddRefProgramState updateAddRefProgramState(Long userId, AddRefProgramState addTariffState);
}
