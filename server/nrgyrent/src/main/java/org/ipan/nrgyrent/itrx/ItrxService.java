package org.ipan.nrgyrent.itrx;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.domain.events.OrderEventPublisher;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@AllArgsConstructor
public class ItrxService {
    public static final int ITRX_ORDER_SUCCESS = 40;
    public static final int ITRX_ORDER_ERROR = 41;
    private final ConcurrentHashMap<UUID, CompletableFuture<OrderCallbackRequest>> correlationResponse = new ConcurrentHashMap<>();

    private final RestClient restClient;
    private final OrderEventPublisher eventPublisher;

    public PlaceOrderResponse placeOrder(Integer energyAmount,String receiveAddress, UUID correlationId) {
        CompletableFuture<OrderCallbackRequest> futureResponse = new CompletableFuture<>();
        correlationResponse.put(correlationId, futureResponse);
        return restClient.placeOrder(energyAmount, "1H", receiveAddress, correlationId.toString());
    }

    public void processCallback(OrderCallbackRequest placeOrderResponse) {
        UUID correlationId = UUID.fromString(placeOrderResponse.out_trade_no);

        CompletableFuture<OrderCallbackRequest> futureResponse = correlationResponse.get(correlationId);
        if (futureResponse != null) {
            futureResponse.complete(placeOrderResponse);
            sendOrderEvent(placeOrderResponse);
        }
    }

    public OrderCallbackRequest getCorrelatedCallbackRequest(UUID correlationId, long timeoutSeconds) {
        CompletableFuture<OrderCallbackRequest> futureResponse = correlationResponse.get(correlationId);
        if (futureResponse != null) {
            try {
                return futureResponse.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                return null;
            } catch (Exception e) {
                // Handle other exceptions
                e.printStackTrace();
            }
        }
        return null;
    }

    private void sendOrderEvent(OrderCallbackRequest orderCallbackRequest) {
        if (orderCallbackRequest.status == ITRX_ORDER_SUCCESS) {
            eventPublisher.publishOrderCompletedEvent(orderCallbackRequest.serial, ITRX_ORDER_SUCCESS, orderCallbackRequest.txid);
        } else if (orderCallbackRequest.status == ITRX_ORDER_ERROR) {
            eventPublisher.publishOrderFailedEvent(orderCallbackRequest.serial, ITRX_ORDER_ERROR);
        }
    }

}
