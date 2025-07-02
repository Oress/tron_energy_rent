package org.ipan.nrgyrent.tron.trongrid;

import java.time.Duration;
import java.util.*;

import org.ipan.nrgyrent.TrongridConfig;
import org.ipan.nrgyrent.tron.trongrid.model.*;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.tron.trident.abi.DefaultFunctionEncoder;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.generated.Uint256;

@Component
@Slf4j
public class TrongridRestClient {
    private static DefaultFunctionEncoder defaultFunctionEncoder = new DefaultFunctionEncoder();

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");

    private final ObjectMapper objectMapper;
    private final TrongridConfig trongridConfig;
    private final Bucket bucket;

    public TrongridRestClient(
            TrongridConfig trongridConfig,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.trongridConfig = trongridConfig;
        this.bucket = Bucket.builder().addLimit(limit -> limit.capacity(1).refillGreedy(trongridConfig.getQps(), Duration.ofSeconds(1))).build();
    }

    // usdtAmount has 6 decimals so 6 UDST = 6_000_000
    public TreeMap<String, Object> transferUsdtSmartContract(String from, String to, long usdtAmount) {
        String responseStr = "";
        try {
            if (tryObtainToken()) {
                String parameters = defaultFunctionEncoder.encodeParameters(List.of(
                        new Address(to),
                        new Uint256(usdtAmount)
                ));

                TreeMap<String, Object> transactionData = new TreeMap<>();
                transactionData.put("owner_address", from);
                transactionData.put("contract_address", trongridConfig.getUsdtAddress());
                transactionData.put("function_selector", "transfer(address,uint256)");
                transactionData.put("parameter", parameters);
                transactionData.put("call_value", 0); // TODO: ????
                transactionData.put("fee_limit", 1000000000); // TODO: ????
                transactionData.put("visible", true);

                String payload = objectMapper.writeValueAsString(transactionData);
                RequestBody body = RequestBody.create(payload, mediaType);
                Request request = new Request.Builder()
                        .url(this.trongridConfig.getBaseUrl() + "/wallet/triggersmartcontract")
                        .method("POST", body)
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                TreeMap<String, Object> map = objectMapper.readValue(responseStr,new TypeReference<TreeMap<String, Object>>() {});
                logger.info("Response" + map);
                return map;
            } else {
                logger.error("Could not obtain bucket4j token for transferUsdtSmartContract");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TreeMap<>();
    }


    public TreeMap<String, Object> createTransaction(String from, String to, long amount) {
        String responseStr = "";
        try {
            if (tryObtainToken()) {
                TreeMap<String, Object> transactionData = new TreeMap<>();
                transactionData.put("owner_address", from);
                transactionData.put("to_address", to);
                transactionData.put("amount", amount);
                transactionData.put("visible", true);

                String payload = objectMapper.writeValueAsString(transactionData);
                RequestBody body = RequestBody.create(payload, mediaType);
                Request request = new Request.Builder()
                        .url(this.trongridConfig.getBaseUrl() + "/wallet/createtransaction")
                        .method("POST", body)
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                TreeMap<String, Object> map = objectMapper.readValue(responseStr,
                        new TypeReference<TreeMap<String, Object>>() {
                        });
                logger.info("Response" + map);
                return map;
            } else {
                logger.error("Could not obtain bucket4j token for createTransaction");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TreeMap<>();
    }

    public TreeMap<String, Object> broadcastTransaction(Map<String, Object> transactionData) {
        String responseStr = "";
        try {
            if (tryObtainToken()) {
                String jsonData = objectMapper.writeValueAsString(transactionData);
                RequestBody body = RequestBody.create(jsonData, mediaType);
                Request request = new Request.Builder()
                        .url(this.trongridConfig.getBaseUrl() + "/wallet/broadcasttransaction")
                        .method("POST", body)
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                TreeMap<String, Object> map = objectMapper.readValue(responseStr,
                        new TypeReference<TreeMap<String, Object>>() {
                        });
                logger.info("broadcastTransaction " + map);
                return map;
            } else {
                logger.error("Could not obtain bucket4j token for broadcastTransaction");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TreeMap<>();
    }

    public List<Transaction> getTransactions(String address,
            Boolean onlyConfirmed,
            Boolean onlyTo,
            Boolean onlyFrom,
            Integer limit) {
        List<Transaction> result = new ArrayList<>();

        String responseStr = "";
        try {
            if (tryObtainToken()) {
                HttpUrl url = HttpUrl.parse(this.trongridConfig.getBaseUrl())
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
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                V1AccountsAddressTransactionsGet200Response responseTyped = objectMapper.readValue(responseStr, V1AccountsAddressTransactionsGet200Response.class);
                result = responseTyped.getData() == null || responseTyped.getData().isEmpty()
                    ? Collections.emptyList()
                    : responseTyped.getData();
            } else {
                logger.error("Could not obtain bucket4j token for getTransactions");
            }
        } catch (Exception e) {
            logger.error("Could not getTransactions response: {}", responseStr, e);
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<TransactionTrc20> getTrc20Transactions(String address,
                                                       Boolean onlyConfirmed,
                                                       Boolean onlyTo,
                                                       Boolean onlyFrom,
                                                       Integer limit) {
        List<TransactionTrc20> result = new ArrayList<>();

        String responseStr = "";
        try {
            if (tryObtainToken()) {
                HttpUrl url = HttpUrl.parse(this.trongridConfig.getBaseUrl())
                        .newBuilder()
                        .addPathSegments("v1/accounts/%s/transactions/trc20".formatted(address))
                        .addQueryParameter("only_confirmed", onlyConfirmed.toString())
                        .addQueryParameter("only_to", onlyTo.toString())
                        // .addQueryParameter("only_from", onlyFrom.toString())
                        .addQueryParameter("limit", limit.toString())
                        .build();


                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                TransactionsTrc20Response responseTyped = objectMapper.readValue(responseStr, TransactionsTrc20Response.class);
                result = responseTyped.getData() == null || responseTyped.getData().isEmpty()
                        ? Collections.emptyList()
                        : responseTyped.getData();
            } else {
                logger.error("Could not obtain bucket4j token for getTransactions");
            }
        } catch (Exception e) {
            logger.error("Could not getTransactions response: {}", responseStr, e);
            throw new RuntimeException(e);
        }

        return result;
    }

    public TreeMap<String, Object> createAccount(String ownerAddress, String address) {
        String responseStr = "";
        try {
            if (tryObtainToken()) {
                TreeMap<String, Object> transactionData = new TreeMap<>();
                transactionData.put("owner_address", ownerAddress);
                transactionData.put("account_address", address);
                transactionData.put("visible", true);

                String jsonData = objectMapper.writeValueAsString(transactionData);
                RequestBody body = RequestBody.create(jsonData, mediaType);
                Request request = new Request.Builder()
                        .url(this.trongridConfig.getBaseUrl() + "/wallet/createaccount")
                        .method("POST", body)
                        .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                checkReponseForFrequencyLimit(response, responseStr);
                TreeMap<String, Object> map = objectMapper.readValue(responseStr, new TypeReference<>() {});
                logger.info("createAccount " + map);
                return map;
            } else {
                logger.error("Could not obtain bucket4j token for createAccount");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new TreeMap<>();
    }

    public AccountInfo getAccountInfo(String depositAddress) {
        AccountInfo result = null;

        String responseStr = "";
        try {
            if (tryObtainToken()) {
            HttpUrl url = HttpUrl.parse(this.trongridConfig.getBaseUrl())
                .newBuilder()
                    .addPathSegments("v1/accounts/%s".formatted(depositAddress))
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("TRON-PRO-API-KEY", this.trongridConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response = client.newCall(request).execute();

            responseStr = response.body().string();
            checkReponseForFrequencyLimit(response, responseStr);
            V1AccountsAddressGet200Response responseTyped = objectMapper.readValue(responseStr, V1AccountsAddressGet200Response.class);
            result = responseTyped.getData() == null || responseTyped.getData().isEmpty()
                ? null
                : responseTyped.getData().get(0);
            } else {
                logger.error("Could not obtain bucket4j token for getAccountInfo");
            }

        } catch (Exception e) {
            logger.error("Could not getAccountInfo response: {}", responseStr, e);
            throw new RuntimeException(e);
        }

        return result;
    }

    private void checkReponseForFrequencyLimit(Response response, String body) {
        if (response.code() != 200) {
            logger.error("Response is not 200: {}, boyd {}", response.code(), body);
        }
    }

    private boolean tryObtainToken() throws InterruptedException {
        return bucket.asBlocking().tryConsume(1, Duration.ofSeconds(40));
    }
}
