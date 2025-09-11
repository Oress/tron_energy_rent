package org.ipan.nrgyrent.catfee;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.bouncycastle.util.encoders.Base64;
import org.ipan.nrgyrent.catfee.dto.CfPlaceOrderResponse;
import org.ipan.nrgyrent.catfee.dto.CfResponse;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.ItrxInsufficientFundsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

@Service
@Slf4j
public class CatfeeRestClient {
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();
    private final MediaType mediaType = MediaType.parse("application/json");
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Value("${app.catfee.base-url}")
    public String baseUrl;
    @Value("${app.catfee.key}")
    public String apiKey;
    @Value("${app.catfee.secret}")
    public String apiSecret;

    @SneakyThrows
    public void config() {
        String timestamp = Instant.now().toString();

        UriComponents uriComponents = UriComponentsBuilder.fromPath("/v1/config").build();

        // Sorting the keys
        String message = timestamp + "GET" + uriComponents;
        String signature = null;
        signature = Base64.toBase64String(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiSecret).hmac(message));
        Request request = new Request.Builder()
                .url(baseUrl + uriComponents)
                .get()
                .addHeader("CF-ACCESS-KEY", apiKey)
                .addHeader("CF-ACCESS-TIMESTAMP", timestamp)
                .addHeader("CF-ACCESS-SIGN", signature)
                .build();
        Response response = client.newCall(request).execute();

        String responseStr = response.body().string();

        logger.info("Response " + responseStr);
//        return placeOrderResponse;
    }


    @SneakyThrows
    public CfResponse<CfPlaceOrderResponse> placeOrder(int energyAmnt, String period, String receiveAddress) {
        String timestamp = Instant.now().toString();

        period = switch (period) {
            case AppConstants.DURATION_1H -> "1h";
            default -> "???";
        };

        UriComponents uriComponents = UriComponentsBuilder.fromPath("/v1/order")
                .queryParam("quantity", energyAmnt)
                .queryParam("receiver", receiveAddress)
                .queryParam("duration", period)
                .build();

        // Sorting the keys
        String message = timestamp + "POST" + uriComponents;
        String signature = null;
        signature = Base64.toBase64String(new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiSecret).hmac(message));

        RequestBody body = RequestBody.create("", mediaType);
        Request request = new Request.Builder()
                .url(baseUrl + uriComponents)
                .method("POST", body)
                .addHeader("CF-ACCESS-KEY", apiKey)
                .addHeader("CF-ACCESS-TIMESTAMP", timestamp)
                .addHeader("CF-ACCESS-SIGN", signature)
                .build();
        Response response = client.newCall(request).execute();

        String responseStr = response.body().string();
        CfResponse<CfPlaceOrderResponse> placeOrderResponse = gson.fromJson(responseStr, new TypeToken<>(){});

        String details = placeOrderResponse.getMsg() != null ? placeOrderResponse.getMsg() : "";

        if (details.contains("balance is too lower")) {
            logger.error("CATFEE balance is insufficient: {}", details);
            throw new ItrxInsufficientFundsException("CATFEE balance is insufficient: " + details);
        }

        if (placeOrderResponse.getCode() != 0) {
            logger.error("Error placing order: {}", placeOrderResponse.getMsg());
            throw new RuntimeException("Error placing order: " + placeOrderResponse.getMsg());
        }

        logger.info("Response " + responseStr);
        return placeOrderResponse;
    }
}

//  "code" : 201,
//  "msg" : "[1a90eae6-cc1c-4dbb-ade5-a1491e158e14]balance is too lower"

//{
//  "code" : 1,
//  "msg" : "duration(1H) is invalid"
//}

/*
* {
  "code" : 0,
  "data" : {
    "id" : "667878c2-dfaa-48c9-9bfe-8a7fc61b746d",
    "resource_type" : "ENERGY",
    "billing_type" : "API",
    "source_type" : "API",
    "pay_timestamp" : 1755440908871,
    "receiver" : "TPCmMJefFV4MvqUpHq1taVuxDXGTRoXTUT",
    "pay_amount_sun" : 4615000,
    "quantity" : 65000,
    "duration" : 60,
    "status" : "PAYMENT_SUCCESS",
    "activate_status" : "ALREADY_ACTIVATED",
    "confirm_status" : "UNCONFIRMED",
    "balance" : 45385000
  }
}
* */