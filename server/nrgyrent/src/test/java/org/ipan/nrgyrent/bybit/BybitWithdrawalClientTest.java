package org.ipan.nrgyrent.bybit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.ipan.nrgyrent.BybitConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BybitWithdrawalClientTest {
    private HttpServer server;
    private BybitRestClient client;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<Headers> headers = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private int status = 200;
    private String response = "{\"retCode\":0,\"retMsg\":\"success\",\"result\":{\"id\":\"123\"}}";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v5/asset/withdraw/create", exchange -> {
            calls.incrementAndGet();
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            headers.set(exchange.getRequestHeaders());
            method.set(exchange.getRequestMethod());
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.getResponseHeaders().add("Location", "/v5/asset/withdraw/create");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        BybitConfig config = mock(BybitConfig.class);
        when(config.getUrlToUse()).thenReturn("http://127.0.0.1:" + server.getAddress().getPort());
        when(config.getApiKey()).thenReturn("test-api-key");
        when(config.getApiSecret()).thenReturn("test-api-secret");
        when(config.getRecvWindow()).thenReturn("5000");
        client = new BybitRestClient(config);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsSignedOnChainTrxRequestWithoutLosingPrecision() throws Exception {
        String requestId = "0123456789abcdef0123456789abcdef";
        var result = client.withdrawTrx("sweep-address", new BigDecimal("123.456789"), requestId);
        assertEquals("123", result.getResult().getId());
        assertEquals(0, result.getRetCode());
        assertEquals("POST", method.get());
        assertEquals(String.valueOf(body.get().getBytes(StandardCharsets.UTF_8).length),
                headers.get().getFirst("Content-Length"));
        JsonObject json = JsonParser.parseString(body.get()).getAsJsonObject();
        assertEquals("TRX", json.get("coin").getAsString());
        assertEquals("TRX", json.get("chain").getAsString());
        assertEquals("sweep-address", json.get("address").getAsString());
        assertEquals("123.456789", json.get("amount").getAsString());
        assertEquals("FUND,UTA", json.get("accountType").getAsString());
        assertEquals(1, json.get("feeType").getAsInt());
        assertEquals(1, json.get("forceChain").getAsInt());
        assertEquals(requestId, json.get("requestId").getAsString());
        String timestamp = headers.get().getFirst("X-BAPI-TIMESTAMP");
        assertEquals(timestamp, json.get("timestamp").getAsString());
        assertEquals("test-api-key", headers.get().getFirst("X-BAPI-API-KEY"));
        assertEquals("5000", headers.get().getFirst("X-BAPI-RECV-WINDOW"));
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec("test-api-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(hmac.doFinal(
                (timestamp + "test-api-key5000" + body.get()).getBytes(StandardCharsets.UTF_8)));
        assertEquals(signature, headers.get().getFirst("X-BAPI-SIGN"));
        assertEquals(1, calls.get());
    }

    @Test
    void preservesBybitRejectionDetails() {
        response = "{\"retCode\":131001,\"retMsg\":\"Insufficient balance\",\"result\":{}}";
        var result = client.withdrawTrx("sweep-address", BigDecimal.TEN, "request1");
        assertEquals(131001, result.getRetCode());
        assertEquals("Insufficient balance", result.getRetMsg());
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {307, 408, 503})
    void neverFollowsRedirectsOrRetriesHttpFailures(int httpStatus) {
        status = httpStatus;
        assertThrows(IOException.class, () -> client.withdrawTrx("sweep-address", BigDecimal.TEN, "request1"));
        assertEquals(1, calls.get());
    }
}
