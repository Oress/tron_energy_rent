package org.ipan.nrgyrent.domain.service.commands.userwallet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteUserWalletCommand {
    private Long walletId;
}
