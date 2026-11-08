package org.ipan.nrgyrent.catfee;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CatfeeRestClientTest {
    private HttpServer server;
    private CatfeeRestClient client;
    private final AtomicReference<Headers> headers = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private int status = 200;
    private String response = """
            {"code":0,"data":{"balance":9007199254740993,"balance_trx":100,"balance_usdt":200}}
            """;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/account", exchange -> {
            headers.set(exchange.getRequestHeaders());
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().toString());
            exchange.getRequestBody().close();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        client = new CatfeeRestClient();
        client.baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client.apiKey = "test-api-key";
        client.apiSecret = "test-api-secret";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void signsAccountRequestAndReadsExactSunBalance() throws Exception {
        var result = client.getAccount();

        assertEquals(0, result.getCode());
        assertEquals(9007199254740993L, result.getData().getBalance());
        assertEquals("GET", method.get());
        assertEquals("/v1/account", path.get());
        assertEquals("application/json", headers.get().getFirst("Content-Type"));
        assertEquals("test-api-key", headers.get().getFirst("CF-ACCESS-KEY"));
        String timestamp = headers.get().getFirst("CF-ACCESS-TIMESTAMP");
        assertDoesNotThrow(() -> Instant.parse(timestamp));

        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec("test-api-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(hmac.doFinal(
                (timestamp + "GET/v1/account").getBytes(StandardCharsets.UTF_8)));
        assertEquals(signature, headers.get().getFirst("CF-ACCESS-SIGN"));
    }

    @Test
    void acceptsZeroBalance() {
        response = "{\"code\":0,\"data\":{\"balance\":0}}";

        assertEquals(0L, client.getAccount().getData().getBalance());
    }

    @Test
    void rejectsApiErrorsEvenWhenHttpStatusIsSuccessful() {
        response = "{\"code\":101,\"msg\":\"Invalid signature\",\"data\":{\"balance\":0}}";

        IOException error = assertThrows(IOException.class, () -> client.getAccount());

        assertTrue(error.getMessage().contains("101"));
        assertTrue(error.getMessage().contains("Invalid signature"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 500})
    void rejectsHttpErrorsEvenWhenBodyContainsBalance(int httpStatus) {
        status = httpStatus;

        IOException error = assertThrows(IOException.class, () -> client.getAccount());

        assertTrue(error.getMessage().contains("HTTP " + httpStatus));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "null", "{}", "{\"data\":{\"balance\":0}}", "{\"code\":0}",
            "{\"code\":0,\"data\":null}", "{\"code\":0,\"data\":{}}",
            "{\"code\":0,\"data\":{\"balance\":null}}",
            "{\"code\":0,\"data\":{\"balance_usdt\":100}}"
    })
    void rejectsIncompleteAccountResponses(String responseBody) {
        response = responseBody;

        assertThrows(IOException.class, () -> client.getAccount());
    }

    @Test
    void rejectsMalformedAccountResponses() {
        response = "<html>Unavailable</html>";

        assertThrows(com.google.gson.JsonParseException.class, () -> client.getAccount());
    }
}
