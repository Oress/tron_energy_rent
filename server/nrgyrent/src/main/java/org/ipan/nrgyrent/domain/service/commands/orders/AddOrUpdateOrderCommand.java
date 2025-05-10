package org.ipan.nrgyrent.domain.service.commands.orders;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddOrUpdateOrderCommand {
    private Long id;
    private Long userId;
    private BigDecimal trxAmount;
    private Integer energyAmount;
    private String receiveAddress;
    private String duration;
    private String correlationId;
    private String serial;
//    private OrderStatus orderStatus;

    private Integer itrxStatus;
    private String txId;

}
