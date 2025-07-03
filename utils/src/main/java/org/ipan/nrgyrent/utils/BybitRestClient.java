package org.ipan.nrgyrent.utils;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
    public void queryAddress() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("coin", "USDT");
        map.put("chainType", "TRC20");

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/deposit/query-address?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();
//        DepositsResponse result = gson.fromJson(string, DepositsResponse.class);

        log.info("Response queryAddress " + string);
//        DepositInner result1 = result.getResult();
    }

    @SneakyThrows
    public void getInteralTransferHistory() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("coin", "USDT");

        String signature = genGetSign(map, timestamp);
        StringBuilder sb = genQueryStr(map);

        Request request = new Request.Builder()
                .url(bybitConfig.getUrlToUse() + "/v5/asset/transfer/query-inter-transfer-list?" + sb)
                .get()
                .addHeader("X-BAPI-API-KEY", bybitConfig.getApiKey())
                .addHeader("X-BAPI-TIMESTAMP", timestamp)
                .addHeader("X-BAPI-SIGN", signature)
                .addHeader("X-BAPI-RECV-WINDOW", bybitConfig.getRecvWindow())
                .addHeader("Content-Type", "application/json")
                .build();
        Response response = client.newCall(request).execute();

        String string = response.body().string();

        log.info("Response getInteralTransferHistory " + string);
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
