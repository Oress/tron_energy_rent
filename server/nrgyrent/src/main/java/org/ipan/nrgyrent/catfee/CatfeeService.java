package org.ipan.nrgyrent.catfee;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.EnergyProvider;
import org.ipan.nrgyrent.catfee.dto.CfPlaceOrderResponse;
import org.ipan.nrgyrent.catfee.dto.CfResponse;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service(AppConstants.PROVIDER_CATFEE)
@Slf4j
@AllArgsConstructor
public class CatfeeService implements EnergyProvider {
    private final OrderEventPublisher eventPublisher;
    private final CatfeeRestClient restClient;

    @Override
    public EstimateOrderAmountResponse estimateOrderPrice(Integer energyAmount, String duration, String receiveAddress) {
        return new EstimateOrderAmountResponse();
    }

    @Override
    public PlaceOrderResponse placeOrder(Integer energyAmount, String duration, String receiveAddress, UUID correlationId) {
//        restClient.config();
        CfResponse<CfPlaceOrderResponse> orderResponse = restClient.placeOrder(energyAmount, duration, receiveAddress);
        CfPlaceOrderResponse data = orderResponse.getData();
        eventPublisher.publishOrderCompletedEvent(
                correlationId.toString(),
                orderResponse.getCode(),
                "",
                data.getId(),
                false,
                data.getReceiver(),
                data.getPay_amount_sun(),
                data.getDuration(),
                data.getQuantity(),
                EnergyProviderName.CATFEE
        );
        eventPublisher.publishBalanceUpdateEvent(EnergyProviderName.CATFEE, data.getBalance());

        return null;
    }
}
