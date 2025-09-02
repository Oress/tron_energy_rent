package org.ipan.nrgyrent.itrx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.ipan.nrgyrent.ItrxConfig;
import org.ipan.nrgyrent.itrx.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class RestClient {
    private static final int ITRX_OK_CODE = 0;

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public final String baseUrl;
    public final String apiKey;
    public final String apiSecret;
    public final String callbackUrl;

    public RestClient(ItrxConfig itrxConfig) {
        this.baseUrl = itrxConfig.getBaseUrl();
        this.apiKey = itrxConfig.getKey();
        this.apiSecret = itrxConfig.getSecret();
        this.callbackUrl = itrxConfig.getCallbackUrl();
    }

    // Rental period, 1H/1D/3D/30D
    @SneakyThrows
    // @Retryable(noRetryFor = {InactiveAddressException.class, ItrxInsufficientFundsException.class})
    public PlaceOrderResponse placeOrder(int energyAmnt, String period, String receiveAddress, String correlationId) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, Object> data = Map.of(
                "energy_amount", energyAmnt,
                "period", period,
                "receive_address", receiveAddress,
                "callback_url", callbackUrl,
                "out_trade_no", correlationId
        );

        // Sorting the keys
        TreeMap<String, Object> sortedData = new TreeMap<>(data);
        String json_data = gson.toJson(sortedData);

        String message = timestamp + "&" + json_data;
        String signature = null;
        signature = Utils.encodeHmacSHA256(message, apiSecret);

        RequestBody body = RequestBody.create(json_data, mediaType);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/frontend/order")
                .method("POST", body)
                .addHeader("API-KEY", apiKey)
                .addHeader("TIMESTAMP", timestamp)
                .addHeader("SIGNATURE", signature)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        PlaceOrderResponse placeOrderResponse = gson.fromJson(response.body().charStream(), PlaceOrderResponse.class);

        String details = placeOrderResponse.getDetail() != null ? placeOrderResponse.getDetail() : "";
        if (details.contains("is the inactive address.")) {
            logger.error("Transaction attempt to inactive address: {} correlation id: {}", details, correlationId);
            throw new InactiveAddressException("Transaction attempt to inactive address: " + details);
        }

        if (details.contains("Insufficient funds")) {
            logger.error("ITRX balance is insufficient: {} correlation id: {}", details, correlationId);
            throw new ItrxInsufficientFundsException("ITRX balance is insufficient: " + details);
        }

        logger.info("Response" + placeOrderResponse);

        if (placeOrderResponse.getErrno() != ITRX_OK_CODE) {
            logger.error("Something went wrong: {} correlation id: {}", placeOrderResponse, correlationId);
            throw new IllegalStateException("Something went wrong");
        }

        return placeOrderResponse;
    }

    @Retryable
    public EstimateOrderAmountResponse estimateOrderPrice(Integer energyAmnt, String period, String receiveAddress) {
        try {
            HttpUrl.Builder builder = HttpUrl.parse(baseUrl + "/api/v1/frontend/order/price").newBuilder();

            if(energyAmnt != null) {
                builder.addQueryParameter("energy_amount", String.valueOf(energyAmnt));
            }
            HttpUrl url = builder
                    .addQueryParameter("period", period)
                    .addQueryParameter("to_address", receiveAddress)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            EstimateOrderAmountResponse placeOrderResponse = gson.fromJson(response.body().charStream(), EstimateOrderAmountResponse.class);
            logger.info("Response" + placeOrderResponse);
            return placeOrderResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public CreateDelegatePolicyResponse createDelegatePolicy(Integer times, String receiveAddress) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, Object> data = Map.of(
                "times", times,
                "receive_address", receiveAddress,
                "unused_times_threshold", "",
                "count_bandwidth_limit", false
        );

        // Sorting the keys
        TreeMap<String, Object> sortedData = new TreeMap<>(data);
        String json_data = gson.toJson(sortedData);

        String message = timestamp + "&" + json_data;
        String signature = null;
        signature = Utils.encodeHmacSHA256(message, apiSecret);

        RequestBody body = RequestBody.create(json_data, mediaType);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/frontend/count-delegate-policy")
                .method("POST", body)
                .addHeader("API-KEY", apiKey)
                .addHeader("TIMESTAMP", timestamp)
                .addHeader("SIGNATURE", signature)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        CreateDelegatePolicyResponse createDelegatePolicyResponse = gson.fromJson(response.body().charStream(), CreateDelegatePolicyResponse.class);
        String details = createDelegatePolicyResponse.getDetail() != null ? createDelegatePolicyResponse.getDetail() : "";
        if (details.contains("is the inactive address.")) {
            logger.error("Transaction attempt to inactive address: {} for AUTO DELEGATION", details);
            throw new InactiveAddressException("Transaction attempt to inactive address: " + details);
        }

        if (details.contains("Insufficient funds")) {
            logger.error("ITRX balance is insufficient: {} for AUTO DELEGATION", details);
            throw new ItrxInsufficientFundsException("ITRX balance is insufficient: " + details);
        }

        logger.info("CreateDelegatePolicyResponse " + createDelegatePolicyResponse);
        return createDelegatePolicyResponse;
    }

    @SneakyThrows
    public DelegatePolicyResponse editDelegatePolicy(String receiveAddress, Boolean pause) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        Map<String, Object> data = Map.of(
                "receive_address", receiveAddress,
                "count_bandwidth_limit", false,
                "count_limit_dynamic", 0,
                "is_always", false,
                "pause", pause
        );

        // Sorting the keys
        TreeMap<String, Object> sortedData = new TreeMap<>(data);
        String json_data = gson.toJson(sortedData);

        String message = timestamp + "&" + json_data;
        String signature = null;
        signature = Utils.encodeHmacSHA256(message, apiSecret);

        RequestBody body = RequestBody.create(json_data, mediaType);
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/frontend/count-delegate-policy/edit-address")
                .method("POST", body)
                .addHeader("API-KEY", apiKey)
                .addHeader("TIMESTAMP", timestamp)
                .addHeader("SIGNATURE", signature)
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        DelegatePolicyResponse createDelegatePolicyResponse = gson.fromJson(response.body().charStream(), DelegatePolicyResponse.class);

        logger.info("DelegatePolicyResponse" + createDelegatePolicyResponse);
        return createDelegatePolicyResponse;
    }

    public ApiUsageResponse getApiStats() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/v1/frontend/userapi/summary")
                    .get()
                    .addHeader("API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = client.newCall(request).execute();

            ApiUsageResponse apiUsageResponse = gson.fromJson(response.body().charStream(), ApiUsageResponse.class);
            logger.info("Api usage response" + apiUsageResponse);
            return apiUsageResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
