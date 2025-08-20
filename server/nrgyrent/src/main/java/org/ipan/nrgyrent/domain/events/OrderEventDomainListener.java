package org.ipan.nrgyrent.domain.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.service.OrderService;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventDomainListener {
    private final OrderService orderService;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;

    @Value("${app.itrx.auto-delegation-threshold}")
    private Long autoDelegateThreshold;

    @EventListener
    @org.springframework.core.annotation.Order(1)
    public void onOrderCompleted(OrderCompletedEvent event) {
        logger.trace("Order completed event received: {}", event);

        if (event.getIsAutoDelegation()) {
            String receiveAddress = event.getReceiveAddress();
            List<AutoDelegationSession> byWalletAndActive = autoDelegationSessionRepo.findByAddressAndActive(receiveAddress, Boolean.TRUE);
            if (byWalletAndActive.size() != 1) {
                logger.error("AUTO DELEGATION. 0 or more than 1 active topup configurations for the wallet {}", receiveAddress);
                return;
            }

            AutoDelegationSession session = byWalletAndActive.get(0);
            AppUser user = session.getUser();
            Tariff tariffToUse = user.getTariffToUse();
            Integer energyAmount = event.getEnergyAmount();
            Long feeSunAmount = event.getAmount();

            Long sunAmount = feeSunAmount <= autoDelegateThreshold
                    ? tariffToUse.getAutodelegateType1AmountSun()
                    : tariffToUse.getAutodelegateType2AmountSun();

            AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                    .correlationId(event.getCorrelationId())
                    .autoDelegationSessionId(session.getId())
                    .userId(user.getTelegramId())
                    .receiveAddress(receiveAddress)
                    .energyAmountPerTx(energyAmount)
                    .txAmount(1)
                    .tariffId(tariffToUse.getId())
                    .sunAmountPerTx(sunAmount)
                    .duration(event.getDuration())
                    .type(OrderType.USER)
                    .energyProvider(EnergyProviderName.ITRX)
                    .itrxFeeSunAmount(feeSunAmount)
                    .itrxStatus(event.getItrxStatus())
                    .txId(event.getTxId())
                    .serial(event.getSerial())
                    .build();
            try {
                orderService.createAutodelegateOrder(command);
            } catch (Exception e) {
                logger.error("Error when createAutodelegateOrder ", e);
            }
        } else {
            AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                    .correlationId(event.getCorrelationId())
                    .itrxStatus(event.getItrxStatus())
                    .txId(event.getTxId())
                    .serial(event.getSerial())
                    .itrxFeeSunAmount(event.getAmount()) // for catfee service
                    .build();
            orderService.completeOrder(command);
        }
    }

    @EventListener
    @org.springframework.core.annotation.Order(1)
    public void onOrderFailed(OrderFailedEvent event) {
        logger.trace("Order failed event received: {}", event);
        AddOrUpdateOrderCommand command = AddOrUpdateOrderCommand.builder()
                .correlationId(event.getCorrelationId())
                .itrxStatus(event.getItrxStatus())
                .serial(event.getSerial())
                .build();
        orderService.refundOrder(command);
    }
}
