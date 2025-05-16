package org.ipan.nrgyrent.itrx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.itrx.dto.EstimateOrderAmountResponse;
import org.ipan.nrgyrent.itrx.dto.PlaceOrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class RestClient {
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Value("${app.itrx.base-url}")
    public String baseUrl;
    @Value("${app.itrx.key}")
    public String apiKey;
    @Value("${app.itrx.secret}")
    public String apiSecret;
    @Value("${app.itrx.callback-url}")
    public String callbackUrl;

    // Rental period, 1H/1D/3D/30D
    public PlaceOrderResponse placeOrder(int energyAmnt, String period, String receiveAddress, String correlationId) {
        try {
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
            logger.info("Response" + placeOrderResponse);
            return placeOrderResponse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public EstimateOrderAmountResponse estimateOrderPrice(int energyAmnt, String period, String receiveAddress) {
        try {
            HttpUrl url = HttpUrl.parse(baseUrl + "/api/v1/frontend/order/price").newBuilder()
                    .addQueryParameter("energy_amount", String.valueOf(energyAmnt))
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
