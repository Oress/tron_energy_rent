package org.ipan.nrgyrent.itrx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

@Component
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
    public void placeOrder(int energyAmnt, String period, String receiveAddress, String correlationId) {
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
            System.out.println("Response" + response.body().string());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
