package org.ipan.nrgyrent.tron.trongrid;

import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@Slf4j
public class TrongridRestClient {
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.trongrid.base-url}")
    public String baseUrl = "https://nile.trongrid.io";
    @Value("${app.trongrid.api-key}")
    public String apiKey;

    public TreeMap<String, Object> createTransaction(String from, String to, long amount) {
        try {
            TreeMap<String, Object> transactionData = new TreeMap<>();
            transactionData.put("owner_address", from);
            transactionData.put("to_address", to);
            transactionData.put("amount", amount);
            transactionData.put("visible", true);

            String payload = objectMapper.writeValueAsString(transactionData);
            RequestBody body = RequestBody.create(payload, mediaType);
            Request request = new Request.Builder()
                    .url(baseUrl + "/wallet/createtransaction")
                    .method("POST", body)
                    .addHeader("TRON-PRO-API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            TreeMap<String, Object> map = objectMapper.readValue(response.body().byteStream(), new TypeReference<TreeMap<String, Object>>() {});
            logger.info("Response" + map);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public TreeMap<String, Object> broadcastTransaction(TreeMap<String, Object> transactionData ) {
        try {
            String jsonData = objectMapper.writeValueAsString(transactionData);
            RequestBody body = RequestBody.create(jsonData, mediaType);
            Request request = new Request.Builder()
                    .url(baseUrl + "/wallet/broadcasttransaction")
                    .method("POST", body)
                    // .addHeader("API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            TreeMap<String, Object> map = objectMapper.readValue(response.body().byteStream(), new TypeReference<TreeMap<String, Object>>() {});
            logger.info("broadcastTransaction " + map);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
