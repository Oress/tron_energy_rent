package org.ipan.nrgyrent.domain.service.commands.orders;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddOrUpdateOrderCommand {
    private Long id;
    private Long userId;
    private Boolean useGroupWallet;
    private Long itrxFeeSunAmount;
    private Long sunAmount;
    private Integer energyAmount;
    private String receiveAddress;
    private String duration;
    private String correlationId;
    private String serial;
//    private OrderStatus orderStatus;

    private Integer itrxStatus;
    private String txId;

}
