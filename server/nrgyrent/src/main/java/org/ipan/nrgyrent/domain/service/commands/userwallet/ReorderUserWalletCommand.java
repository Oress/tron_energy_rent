package org.ipan.nrgyrent.domain.service.commands.userwallet;

import lombok.Data;

import java.util.List;

@Data
public class ReorderUserWalletCommand {
    private List<Long> idsInNewOrder;
}
