package org.ipan.nrgyrent.netts;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.ipan.nrgyrent.itrx.ItrxInsufficientFundsException;
import org.ipan.nrgyrent.netts.dto.NettsPlaceOrderRequest;
import org.ipan.nrgyrent.netts.dto.NettsPlaceOrderResponse200;
import org.ipan.nrgyrent.netts.dto.NettsUserInfoResponse200;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class NettsRestClient {
    private final OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(30, TimeUnit.SECONDS).build();
    private final MediaType mediaType = MediaType.parse("application/json");
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Value("${app.netts.base-url}")
    public String baseUrl;
    @Value("${app.netts.key}")
    public String apiKey;
    @Value("${app.netts.real-ip}")
    public String realIp;

    @SneakyThrows
    public NettsUserInfoResponse200 getStats() {
        UriComponents uriComponents = UriComponentsBuilder.fromPath("/apiv2/userinfo").build();

        Request request = new Request.Builder()
                .url(baseUrl + uriComponents)
                .method("GET", null)
                .addHeader("X-API-KEY", apiKey)
                .addHeader("X-Real-IP", realIp)
                .build();
        Response response = client.newCall(request).execute();
        String responseStr = response.body().string();
        logger.info("NETTS.IO user info: {}", responseStr);

        int responseCode = response.code();
        if (responseCode != 200) {
            logger.error("NETTS.IO Error getting user info");
            throw new RuntimeException("Error getting user info");
        }
        NettsUserInfoResponse200 userInfoResponse = gson.fromJson(responseStr, NettsUserInfoResponse200.class);
        return userInfoResponse;
    }

    // only 1 hour period is supported
    @SneakyThrows
    public NettsPlaceOrderResponse200 placeOrder(int energyAmnt, String period, String receiveAddress) {
        UriComponents uriComponents = UriComponentsBuilder.fromPath("/apiv2/order1h").build();

        var req = new NettsPlaceOrderRequest(energyAmnt, receiveAddress);
        String jsonMap = JSON.toJSONString(req);
        RequestBody body = RequestBody.create(jsonMap, mediaType);
        Request request = new Request.Builder()
                .url(baseUrl + uriComponents)
                .method("POST", body)
                .addHeader("X-API-KEY", apiKey)
                .addHeader("X-Real-IP", realIp)
                .build();
        Response response = client.newCall(request).execute();
        String responseStr = response.body().string();
        logger.info("NETTS.IO place order info: {}", responseStr);

        // https://doc.netts.io/api/v2/endpoints/order1h.html#error-responses
        int responseCode = response.code();
        if (responseCode == 403) {
            logger.error("NETTS.IO balance is insufficient");
            throw new ItrxInsufficientFundsException("NETTS.IO balance is insufficient: " + responseStr);
        } else if (responseCode != 200) {
            logger.error("NETTS.IO Error placing order");
            throw new RuntimeException("Error placing order");
        }
        NettsPlaceOrderResponse200 placeOrderResponse = gson.fromJson(responseStr, NettsPlaceOrderResponse200.class);
        return placeOrderResponse;
    }
}

// Example of invalid request
/*
{
  "detail": {
    "code": -1,
    "msg": "Invalid API key or IP not in whitelist"
  }
}
*/

// 200 response for userInfo
/*
{
  "status": "success",
  "timestamp": "2011-11-11 01:18:02",
  "user_id": 1111,
  "user_info": {
    "email": "*****",
    "name": ""*****",
    "deposit_address": ""*****"
  },
  "stats": {
    "balance": 11.0,
    "total_delegations": 0,
    "total_energy_delegated": 0,
    "total_trx_spent": 0,
    "total_deposit": 0,
    "avg_rate_sun_energy": 0,
    "save_by_netts_percent": 0,
    "save_in_dollars": 0
  },
  "network_info": {
    "trx_price": 0.2969,
    "network_energy_fee": 100.0
  }
}

*/// 200 response for userInfo
/*
{
  "status": "success",
  "timestamp": "2011-11-11 01:18:02",
  "user_id": 1111,
  "user_info": {
    "email": "*****",
    "name": ""*****",
    "deposit_address": ""*****"
  },
  "stats": {
    "balance": 11.0,
    "total_delegations": 0,
    "total_energy_delegated": 0,
    "total_trx_spent": 0,
    "total_deposit": 0,
    "avg_rate_sun_energy": 0,
    "save_by_netts_percent": 0,
    "save_in_dollars": 0
  },
  "network_info": {
    "trx_price": 0.2969,
    "network_energy_fee": 100.0
  }
}
*/