package org.ipan.nrgyrent.bybit;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ipan.nrgyrent.BybitConfig;
import org.ipan.nrgyrent.bybit.dto.*;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Component
@Slf4j
@AllArgsConstructor
public class BybitRestClient {
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");

    private final BybitConfig bybitConfig;

    @SneakyThrows
    public void convertResultQuery() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("accountType","eb_convert_uta");

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/exchange/query-coin-list?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
//        PlaceOrderResponse placeOrderResponse = gson.fromJson(response.body().charStream(), PlaceOrderResponse.class);

        logger.info("Response" + string);
//        return placeOrderResponse;
    }

    @SneakyThrows
    public QuoteCheck convertResultQuery(String quoteId) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("quoteTxId", quoteId);
        map.put("accountType","eb_convert_funding");

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/exchange/convert-result-query?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        QuoteCheck result = gson.fromJson(string, QuoteCheck.class);

        logger.info("Response" + string);
        return result;
    }

    @SneakyThrows
    public QuoteApply quoteApply(BigDecimal amount) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("accountType","eb_convert_funding");
        map.put("requestCoin", "USDC");
        map.put("fromCoin", "USDC");
        map.put("toCoin", "USDT");
        map.put("requestAmount", amount.setScale(2, RoundingMode.DOWN).toString());

        String signature = genPostSign(map, timestamp);
        String jsonMap = JSON.toJSONString(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/exchange/quote-apply")
                .method("POST", RequestBody.create(jsonMap, mediaType))
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-SIGN-TYPE", "2")
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        QuoteApply result = gson.fromJson(string, QuoteApply.class);

        logger.info("Response" + string);
        return result;
    }

    @SneakyThrows
    public QuoteConfirm confirmQuote(String quoteId) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("quoteTxId",quoteId);

        String signature = genPostSign(map, timestamp);
        String jsonMap = JSON.toJSONString(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/exchange/convert-execute")
                .method("POST", RequestBody.create(jsonMap, mediaType))
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-SIGN-TYPE", "2")
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        QuoteConfirm result = gson.fromJson(string, QuoteConfirm.class);

        logger.info("Response" + string);
        return result;
    }

    @SneakyThrows
    public InternalTransferResponse internalTransfer(String fromType, String toType, BigDecimal amount, String coin) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("transferId",UUID.randomUUID().toString());
        map.put("fromAccountType", fromType);
        map.put("toAccountType", toType);
        map.put("amount", amount.toString());
        map.put("coin", coin);

        String signature = genPostSign(map, timestamp);
        String jsonMap = JSON.toJSONString(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/transfer/inter-transfer")
                .method("POST", RequestBody.create(jsonMap, mediaType))
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-SIGN-TYPE", "2")
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        InternalTransferResponse result = gson.fromJson(string, InternalTransferResponse.class);

        logger.info("Response" + string);
        return result;
    }

    @SneakyThrows
    public PlaceOrderResponse placeMarketOrderTRXUSDT(BigDecimal qtyUsdt) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("category", "spot");
        map.put("symbol", "TRXUSDT");
        map.put("orderType", "Market");
        map.put("side", "Buy");
        map.put("qty", qtyUsdt.toString());

        String signature = genPostSign(map, timestamp);
        String jsonMap = JSON.toJSONString(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/order/create")
                .method("POST", RequestBody.create(jsonMap, mediaType))
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-SIGN-TYPE", "2")
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        PlaceOrderResponse result = gson.fromJson(string, PlaceOrderResponse.class);

        logger.info("Response" + string);
        return result;
    }

    @SneakyThrows
    public GetOrderData getOrderStatus(String orderId) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("category", "spot");
        map.put("orderId", orderId);

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/order/realtime?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        GetOrderResponse result = gson.fromJson(string, GetOrderResponse.class);

        logger.info("Response" + string);
        return result.getResult().getList().get(0);
    }

    @SneakyThrows
    public DepositData getUsdtDeposits(String txId) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("coin", "USDT");
        if (txId != null) {
            map.put("txID", txId);
        }

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/deposit/query-record?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
        DepositsResponse result = gson.fromJson(string, DepositsResponse.class);

        logger.info("Response getUsdtDeposits " + string);
        DepositInner result1 = result.getResult();
        return result1 != null && result1.getRows() != null && !result1.getRows().isEmpty()
                ? result1.getRows().get(0)
                : null;
    }

    private String genPostSign(Map<String, Object> params, String timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(bybitConfig.getApiSecret().getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String paramJson = JSON.toJSONString(params);
        String sb = timestamp + bybitConfig.getApiKey() + bybitConfig.getRecvWindow() + paramJson;
        return bytesToHex(sha256_HMAC.doFinal(sb.getBytes()));
    }

    /**
     * The way to generate the sign for GET requests
     * @param params: Map input parameters
     * @return signature used to be a parameter in the header
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    private String genGetSign(Map<String, Object> params, String timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        StringBuilder sb = genQueryStr(params);
        String queryStr = timestamp + bybitConfig.getApiKey() + bybitConfig.getRecvWindow() + sb;

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(bybitConfig.getApiSecret().getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return bytesToHex(sha256_HMAC.doFinal(queryStr.getBytes()));
    }

    /**
     * To convert bytes to hex
     * @param hash
     * @return hex string
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * To generate query string for GET requests
     * @param map
     * @return
     */
    private static StringBuilder genQueryStr(Map<String, Object> map) {
        Set<String> keySet = map.keySet();
        Iterator<String> iter = keySet.iterator();
        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            String key = iter.next();
            sb.append(key)
                    .append("=")
                    .append(map.get(key))
                    .append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb;
    }
}
