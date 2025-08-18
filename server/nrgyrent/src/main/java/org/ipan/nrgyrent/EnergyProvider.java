package org.ipan.nrgyrent;

import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;

import java.util.UUID;

public interface EnergyProvider {
    EstimateOrderAmountResponse estimateOrderPrice(Integer energyAmount, String duration, String receiveAddress);
    PlaceOrderResponse placeOrder(Integer energyAmount, String duration, String receiveAddress, UUID correlationId);
}
