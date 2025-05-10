package org.ipan.nrgyrent.domain.service.commands.userwallet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddOrUpdateUserWalletCommand {
    private Long id; // may be null for new wallet
    private Long userId;
//    private String label;
    private String walletAddress;
}
