package org.ipan.nrgyrent.itrx;

import lombok.AllArgsConstructor;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@AllArgsConstructor
public class ItrxService {
    private final ConcurrentHashMap<UUID, CompletableFuture<OrderCallbackRequest>> correlationResponse = new ConcurrentHashMap<>();

    private final RestClient restClient;

    public PlaceOrderResponse placeOrder(Integer energyAmount,String receiveAddress, UUID correlationId) {
        CompletableFuture<OrderCallbackRequest> futureResponse = new CompletableFuture<>();
        correlationResponse.put(correlationId, futureResponse);
        return restClient.placeOrder(energyAmount, "1H", receiveAddress, correlationId.toString());
    }

    public void setPlaceOrderResponse(UUID correlationId, OrderCallbackRequest placeOrderResponse) {
        CompletableFuture<OrderCallbackRequest> futureResponse = correlationResponse.get(correlationId);
        if (futureResponse != null) {
            futureResponse.complete(placeOrderResponse);
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
}
