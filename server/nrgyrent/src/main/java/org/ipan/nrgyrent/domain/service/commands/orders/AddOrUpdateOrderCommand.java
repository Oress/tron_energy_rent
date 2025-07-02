package org.ipan.nrgyrent.domain.service.commands.orders;

import lombok.Builder;
import lombok.Data;
import org.ipan.nrgyrent.domain.model.OrderType;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEventType;

@Data
@Builder
public class AddOrUpdateOrderCommand {
    private Long id;
    private Long chatId;
    private Integer messageIdToUpdate;
    private Long userId;
    private Long itrxFeeSunAmount;
    private Long sunAmountPerTx;
    private Integer txAmount;
    private Integer energyAmountPerTx;
    private String receiveAddress;
    private String duration;
    private String correlationId;
    private String serial;
    private Long tariffId;
    private OrderType type;

    private Long autoDelegationSessionId;
    private AutoDelegationEventType delegationEventType;
//    private OrderStatus orderStatus;

    private Integer itrxStatus;
    private String txId;

}
