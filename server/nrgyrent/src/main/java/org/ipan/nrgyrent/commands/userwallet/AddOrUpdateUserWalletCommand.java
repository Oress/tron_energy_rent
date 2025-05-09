package org.ipan.nrgyrent.commands.userwallet;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddOrUpdateUserWalletCommand {
    private Long id; // may be null for new wallet
//    private String label;
    private String walletAddress;
}
