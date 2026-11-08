package org.ipan.nrgyrent.fixedfloat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSink;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class FixedFloatClient {
    private final FixedFloatConfig config;
    private final FixedFloatRateLimiter rateLimiter;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    // Creating an order is not idempotent. Never replay POST requests or forward credentials on redirects.
    private final OkHttpClient http = new OkHttpClient.Builder()
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
            .callTimeout(30, TimeUnit.SECONDS).build();

    @Autowired
    public FixedFloatClient(FixedFloatConfig config) {
        this(config, new FixedFloatRateLimiter());
    }

    FixedFloatClient(FixedFloatConfig config, FixedFloatRateLimiter rateLimiter) {
        this.config = config;
        this.rateLimiter = rateLimiter;
    }

    public FixedFloatOrder createOrder(BigDecimal amount, String payoutAddress) throws IOException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "float");
        params.put("fromCcy", "USDTTRC");
        params.put("toCcy", "TRX");
        params.put("direction", "from");
        params.put("amount", amount);
        params.put("toAddress", payoutAddress);
        String refcode = config.getRefcode();
        if (refcode != null && !refcode.isBlank()) {
            params.put("refcode", refcode.strip());
        }
        // The native float rate includes 0.5%; afftax would add an extra affiliate charge.
        return request("create", params);
    }

    public FixedFloatOrder getOrder(String id, String token) throws IOException {
        return request("order", Map.of("id", id, "token", token));
    }

    private FixedFloatOrder request(String method, Map<String, Object> params) throws IOException {
        if (config.getApiKey() == null || config.getApiKey().isBlank()
                || config.getApiSecret() == null || config.getApiSecret().isBlank()) {
            throw new IOException("FixedFloat API credentials are not configured");
        }
        byte[] body = gson.toJson(params).getBytes(StandardCharsets.UTF_8);
        String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
                config.getApiSecret().getBytes(StandardCharsets.UTF_8)).hmacHex(body);
        RequestBody requestBody = new RequestBody() {
            @Override public MediaType contentType() { return MediaType.parse("application/json; charset=UTF-8"); }
            @Override public long contentLength() { return body.length; }
            @Override public boolean isOneShot() { return true; }
            @Override public void writeTo(BufferedSink sink) throws IOException { sink.write(body); }
        };
        Request request = new Request.Builder().url(config.getBaseUrl() + "/" + method)
                .header("X-API-KEY", config.getApiKey()).header("X-API-SIGN", signature)
                .header("Accept", "application/json")
                .post(requestBody)
                .build();
        // ff.io permits 250 weight units/minute: create costs 50, order costs 1.
        // Charge failed requests too; a timeout does not prove the provider did not receive them.
        long queuedAt = System.nanoTime();
        logger.debug("FixedFloat API queued: method={}, orderId={}", method, params.get("id"));
        rateLimiter.acquire("create".equals(method) ? 50 : 1);
        long startedAt = System.nanoTime();
        logger.debug("FixedFloat API sending: method={}, orderId={}, budgetWaitMs={}",
                method, params.get("id"), TimeUnit.NANOSECONDS.toMillis(startedAt - queuedAt));
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                logger.warn("FixedFloat API HTTP failure: method={}, orderId={}, httpStatus={}, hasBody={}",
                        method, params.get("id"), response.code(), response.body() != null);
                throw new IOException("FixedFloat " + method + " failed: HTTP " + response.code());
            }
            JsonObject envelope;
            try {
                envelope = gson.fromJson(response.body().string(), JsonObject.class);
                if (envelope == null || !envelope.has("code") || envelope.get("code").isJsonNull()) {
                    throw new IOException("Invalid FixedFloat response: missing code");
                }
                int code = envelope.get("code").getAsBigDecimal().intValueExact();
                if (code != 0) {
                    logger.warn("FixedFloat API rejected request: method={}, orderId={}, apiCode={}",
                            method, params.get("id"), code);
                    throw new IOException("FixedFloat " + method + " failed: code " + code);
                }
                FixedFloatOrder order = gson.fromJson(envelope.get("data"), FixedFloatOrder.class);
                if (order == null || order.id() == null || order.id().isBlank()
                        || order.status() == null || order.status().isBlank()) {
                    throw new IOException("Invalid FixedFloat order response");
                }
                logger.debug("FixedFloat API completed: method={}, orderId={}, providerStatus={}, elapsedMs={}",
                        method, order.id(), order.status(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
                return order;
            } catch (RuntimeException e) {
                // Do not include response bodies or tokens in exceptions.
                logger.warn("FixedFloat API invalid response: method={}, orderId={}, errorType={}",
                        method, params.get("id"), e.getClass().getSimpleName());
                throw new IOException("Invalid FixedFloat response");
            }
        } catch (IOException e) {
            // Upstream exception messages and response bodies may contain credentials or order tokens.
            logger.warn("FixedFloat API request failed: method={}, orderId={}, elapsedMs={}, errorType={}",
                    method, params.get("id"), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt),
                    e.getClass().getSimpleName());
            throw e;
        }
    }
}
