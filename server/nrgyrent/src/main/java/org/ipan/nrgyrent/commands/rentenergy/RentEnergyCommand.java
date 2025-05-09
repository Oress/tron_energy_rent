package org.ipan.nrgyrent.commands.rentenergy;

import lombok.Data;

@Data
public class RentEnergyCommand {
    private Integer amount;
    private Integer durationHours;
    private String recipientWalletAddressBase58;
}
