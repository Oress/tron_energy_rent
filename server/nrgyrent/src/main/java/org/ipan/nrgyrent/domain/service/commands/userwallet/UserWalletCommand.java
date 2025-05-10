package org.ipan.nrgyrent.domain.service.commands.userwallet;

import lombok.Data;

@Data
public class UserWalletCommand {
    private AddOrUpdateUserWalletCommand add;
    private AddOrUpdateUserWalletCommand update;
    private DeleteUserWalletCommand delete;
    private ReorderUserWalletCommand reorder;
}
