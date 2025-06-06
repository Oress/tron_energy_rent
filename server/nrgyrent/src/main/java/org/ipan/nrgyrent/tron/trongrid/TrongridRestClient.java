package org.ipan.nrgyrent.tron.trongrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.ipan.nrgyrent.tron.trongrid.model.AccountInfo;
import org.ipan.nrgyrent.tron.trongrid.model.Transaction;
import org.ipan.nrgyrent.tron.trongrid.model.V1AccountsAddressGet200Response;
import org.ipan.nrgyrent.tron.trongrid.model.V1AccountsAddressTransactionsGet200Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
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

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final RateLimiter rateLimiter;

    public TrongridRestClient(
            ObjectMapper objectMapper,
            @Value("${app.trongrid.qps:15}") Integer qps,
            @Value("${app.trongrid.base-url:https://nile.trongrid.io}") String baseUrl,
            @Value("${app.trongrid.api-key}") String apiKey) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.rateLimiter = RateLimiter.create(qps);
    }

    public TreeMap<String, Object> createTransaction(String from, String to, long amount) {
        try {
            rateLimiter.acquire();
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

            TreeMap<String, Object> map = objectMapper.readValue(response.body().byteStream(),
                    new TypeReference<TreeMap<String, Object>>() {
                    });
            logger.info("Response" + map);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TreeMap<String, Object> broadcastTransaction(TreeMap<String, Object> transactionData) {
        try {
            rateLimiter.acquire();
            String jsonData = objectMapper.writeValueAsString(transactionData);
            RequestBody body = RequestBody.create(jsonData, mediaType);
            Request request = new Request.Builder()
                    .url(baseUrl + "/wallet/broadcasttransaction")
                    .method("POST", body)
                    .addHeader("TRON-PRO-API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            TreeMap<String, Object> map = objectMapper.readValue(response.body().byteStream(),
                    new TypeReference<TreeMap<String, Object>>() {
                    });
            logger.info("broadcastTransaction " + map);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Transaction> getTransactions(String address,
            Boolean onlyConfirmed,
            Boolean onlyTo,
            Boolean onlyFrom, 
            Integer limit) {
        List<Transaction> result = new ArrayList<>();

        try {
            rateLimiter.acquire();

            HttpUrl url = HttpUrl.parse(this.baseUrl)
                .newBuilder()
                    .addPathSegments("v1/accounts/%s/transactions".formatted(address))
                    .addQueryParameter("only_confirmed", onlyConfirmed.toString())
                    .addQueryParameter("only_to", onlyTo.toString())
                    // .addQueryParameter("only_from", onlyFrom.toString())
                    .addQueryParameter("limit", limit.toString())
                    .build();


            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("TRON-PRO-API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            V1AccountsAddressTransactionsGet200Response responseTyped = objectMapper.readValue(response.body().byteStream(), V1AccountsAddressTransactionsGet200Response.class);
            result = responseTyped.getData() == null || responseTyped.getData().isEmpty()
                ? Collections.emptyList()
                : responseTyped.getData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public AccountInfo getAccountInfo(String depositAddress) {
        AccountInfo result = null;

        try {
            rateLimiter.acquire();

            HttpUrl url = HttpUrl.parse(this.baseUrl)
                .newBuilder()
                    .addPathSegments("v1/accounts/%s".formatted(depositAddress))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("TRON-PRO-API-KEY", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            V1AccountsAddressGet200Response responseTyped = objectMapper.readValue(response.body().byteStream(), V1AccountsAddressGet200Response.class);
            result = responseTyped.getData() == null || responseTyped.getData().isEmpty()
                ? null
                : responseTyped.getData().get(0);
        } catch (Exception e) {
            logger.error("Could not getAccountInfo", e);
            throw new RuntimeException(e);
        }

        return result;
    }
}
