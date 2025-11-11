package org.ipan.nrgyrent.netts;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.EnergyProvider;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.Utils;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.ipan.nrgyrent.netts.dto.NettsPlaceOrderResponse200;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service(AppConstants.PROVIDER_NETTS)
@Slf4j
@AllArgsConstructor
public class NettsService implements EnergyProvider {
    private final OrderEventPublisher eventPublisher;
    private final NettsRestClient restClient;

    @Override
    public EstimateOrderAmountResponse estimateOrderPrice(Integer energyAmount, String duration, String receiveAddress) {
        return new EstimateOrderAmountResponse();
    }

    @Override
    public PlaceOrderResponse placeOrder(Integer energyAmount, String duration, String receiveAddress, UUID correlationId) {
        NettsPlaceOrderResponse200 orderResponse = restClient.placeOrder(energyAmount, duration, receiveAddress);
        NettsPlaceOrderResponse200.DetailResponse detail = orderResponse.getDetail();
        NettsPlaceOrderResponse200.DataResponse data = detail.getData();
        eventPublisher.publishOrderCompletedEvent(
                correlationId.toString(),
                detail.getCode(),
                data.getHash(),
                data.getOrderId(),
                false,
                data.getDelegateAddress(),
                Utils.trxToSun(data.getPaidTRX()),
                AppConstants.DURATION_1H,
                data.getEnergy(),
                EnergyProviderName.NETTS
        );

        return new PlaceOrderResponse();
    }
}
