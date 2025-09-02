package org.ipan.nrgyrent.itrx;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ipan.nrgyrent.EnergyProvider;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service(AppConstants.PROVIDER_ITRX)
@AllArgsConstructor
@Slf4j
public class ItrxService implements EnergyProvider {
    public static final int ITRX_ORDER_SUCCESS = 40;
    public static final int ITRX_ORDER_ERROR = 41;
    private final ConcurrentHashMap<UUID, CompletableFuture<OrderCallbackRequest>> correlationResponse = new ConcurrentHashMap<>();

    private final RestClient restClient;
    private final OrderEventPublisher eventPublisher;

    public EstimateOrderAmountResponse estimateOrderPrice(Integer energyAmount, String duration, String receiveAddress) {
        logger.trace("Estimating order price for energyAmount: {}, duration: {}, receiveAddress: {}", energyAmount, duration, receiveAddress);
        return restClient.estimateOrderPrice(energyAmount, duration, receiveAddress);
    }

    public PlaceOrderResponse placeOrder(Integer energyAmount, String duration, String receiveAddress, UUID correlationId) {
        logger.trace("Placing order for energyAmount: {}, duration: {}, receiveAddress: {}, correlationId: {}", energyAmount, duration, receiveAddress, correlationId);
        CompletableFuture<OrderCallbackRequest> futureResponse = new CompletableFuture<>();
        correlationResponse.put(correlationId, futureResponse);
        return restClient.placeOrder(energyAmount, duration, receiveAddress, correlationId.toString());
    }

    public void processCallback(OrderCallbackRequest placeOrderResponse, EnergyProviderName energyProvider) {
        logger.info("Processing callback: {}", placeOrderResponse);
        if (placeOrderResponse.out_trade_no != null) {
            UUID correlationId = UUID.fromString(placeOrderResponse.out_trade_no);
            logger.info("Correlation ID: {}", correlationId);
        }

/*        CompletableFuture<OrderCallbackRequest> futureResponse = correlationResponse.get(correlationId);
        if (futureResponse != null) {
            logger.info("Completing future response for correlation ID: {}", correlationId);
            futureResponse.complete(placeOrderResponse);
        } else {
            logger.error("No future response found for correlation ID: {}", correlationId);
        }*/
        sendOrderEvent(placeOrderResponse, energyProvider);
    }

    public OrderCallbackRequest getCorrelatedCallbackRequest(UUID correlationId, long timeoutSeconds) {
        logger.trace("Getting correlated callback request for correlation ID: {}", correlationId);
        CompletableFuture<OrderCallbackRequest> futureResponse = correlationResponse.get(correlationId);
        if (futureResponse != null) {
            try {
                return futureResponse.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Timeout while waiting for correlated callback request for correlation ID: {}", correlationId);
                return null;
            } catch (Exception e) {
                // Handle other exceptions
                e.printStackTrace();
            }
        }
        return null;
    }

    private void sendOrderEvent(OrderCallbackRequest orderCallbackRequest, EnergyProviderName energyProvider) {
        if (orderCallbackRequest.status == ITRX_ORDER_SUCCESS) {
            String correlationId = orderCallbackRequest.out_trade_no;
            if (orderCallbackRequest.isAutoDelegate()) {
                correlationId = UUID.randomUUID().toString();
            }

            eventPublisher.publishOrderCompletedEvent(
                    correlationId,
                    ITRX_ORDER_SUCCESS,
                    orderCallbackRequest.txid,
                    orderCallbackRequest.serial,
                    orderCallbackRequest.isAutoDelegate(),
                    orderCallbackRequest.getReceive_address(),
                    orderCallbackRequest.getAmount(),
                    orderCallbackRequest.getPeriod(),
                    orderCallbackRequest.getEnergy_amount(),
                    energyProvider
            );
        } else if (orderCallbackRequest.status == ITRX_ORDER_ERROR) {
            eventPublisher.publishOrderFailedEvent(orderCallbackRequest.out_trade_no, ITRX_ORDER_ERROR, orderCallbackRequest.serial);
        }
    }

}
