package org.ipan.nrgyrent.fixedfloat;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class FixedFloatClientTest {
    private HttpServer server;
    private FixedFloatClient client;
    private FixedFloatConfig config;
    private String requestBody;
    private String signature;
    private String apiKey;
    private String path;
    private String method;
    private String contentType;
    private String contentLength;
    private String accept;
    private int httpStatus = 200;
    private String response = """
            {"code":0,"data":{"id":"ABC123","token":"secret-token","type":"float","status":"NEW",
             "from":{"code":"USDTTRC","network":"TRX","amount":"8.500001","address":"from"},
             "to":{"code":"TRX","network":"TRX","amount":"9007199254.740993","address":"to"}}}
            """;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            signature = exchange.getRequestHeaders().getFirst("X-API-SIGN");
            apiKey = exchange.getRequestHeaders().getFirst("X-API-KEY");
            path = exchange.getRequestURI().getPath();
            method = exchange.getRequestMethod();
            contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
            accept = exchange.getRequestHeaders().getFirst("Accept");
            exchange.getResponseHeaders().add("Retry-After", "0");
            exchange.getResponseHeaders().add("Location", "/create");
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(httpStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        config = new FixedFloatConfig();
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v2");
        config.setApiKey("key");
        config.setApiSecret("secret");
        client = new FixedFloatClient(config);
    }

    @AfterEach void cleanup() { server.stop(0); }

    @Test void createsSignedFloatOrderWithoutExtraCommission() throws Exception {
        FixedFloatOrder order = client.createOrder(new BigDecimal("8.500001"), "payout");
        var json = JsonParser.parseString(requestBody).getAsJsonObject();
        assertEquals("/api/v2/create", path);
        assertEquals("POST", method);
        assertEquals("application/json; charset=UTF-8", contentType);
        assertEquals("application/json", accept);
        assertEquals(String.valueOf(requestBody.getBytes(StandardCharsets.UTF_8).length), contentLength);
        assertEquals("float", json.get("type").getAsString());
        assertEquals("USDTTRC", json.get("fromCcy").getAsString());
        assertEquals("TRX", json.get("toCcy").getAsString());
        assertEquals("from", json.get("direction").getAsString());
        assertEquals("payout", json.get("toAddress").getAsString());
        assertEquals(new BigDecimal("8.500001"), json.get("amount").getAsBigDecimal());
        assertFalse(json.has("afftax"));
        assertFalse(json.has("refcode"));
        assertEquals("key", apiKey);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertEquals(HexFormat.of().formatHex(mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8))), signature);
        assertEquals(new BigDecimal("9007199254.740993"), order.to().amount());
        assertFalse(order.toString().contains("secret-token"));
    }

    @ParameterizedTest @ValueSource(strings = {"ap3ea7ba", "  ap3ea7ba\t"})
    void includesConfiguredReferralCodeInSignedOrderWithoutAffiliateMarkup(String refcode) throws Exception {
        config.setRefcode(refcode);
        client.createOrder(BigDecimal.TEN, "payout");
        var json = JsonParser.parseString(requestBody).getAsJsonObject();
        assertEquals("ap3ea7ba", json.get("refcode").getAsString());
        assertFalse(json.has("afftax"));
        assertEquals("float", json.get("type").getAsString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertEquals(HexFormat.of().formatHex(mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8))), signature);
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" \t\n"})
    void omitsUnconfiguredReferralCode(String refcode) throws Exception {
        config.setRefcode(refcode);
        client.createOrder(BigDecimal.TEN, "payout");
        var json = JsonParser.parseString(requestBody).getAsJsonObject();
        assertFalse(json.has("refcode"));
        assertFalse(json.has("afftax"));
    }

    @Test void pollsWithPersistedIdAndToken() throws Exception {
        config.setRefcode("ap3ea7ba");
        client.getOrder("ABC123", "secret-token");
        var json = JsonParser.parseString(requestBody).getAsJsonObject();
        assertEquals("/api/v2/order", path);
        assertEquals("POST", method);
        assertEquals("ABC123", json.get("id").getAsString());
        assertEquals("secret-token", json.get("token").getAsString());
        assertFalse(json.has("refcode"));
        assertFalse(json.has("afftax"));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertEquals(HexFormat.of().formatHex(mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8))), signature);
    }

    @Test void readsDocumentedNullTransactionFieldsAndOptionalPollingToken() throws Exception {
        JsonObject envelope = JsonParser.parseString(response).getAsJsonObject();
        JsonObject data = envelope.getAsJsonObject("data");
        data.remove("token"); // The order response does not require returning the access token again.
        data.add("time", JsonParser.parseString("{\"start\":null,\"finish\":null,\"left\":1800}"));
        for (String side : new String[]{"from", "to"}) {
            data.getAsJsonObject(side).add("tx", JsonParser.parseString("{\"id\":null,\"amount\":null,\"fee\":null}"));
        }
        response = envelope.toString();
        FixedFloatOrder order = client.getOrder("ABC123", "secret-token");
        assertEquals("NEW", order.status());
        assertNull(order.token());
        assertNull(order.from().tx().id());
        assertNull(order.to().tx().amount());
    }

    @Test void readsFinalTransactionAmountSeparatelyFromQuotedAmount() throws Exception {
        JsonObject envelope = JsonParser.parseString(response).getAsJsonObject();
        JsonObject data = envelope.getAsJsonObject("data");
        data.addProperty("status", "DONE");
        data.getAsJsonObject("to").add("tx", JsonParser.parseString("{\"id\":\"trx-tx\",\"amount\":\"28.123456\"}"));
        response = envelope.toString();
        FixedFloatOrder order = client.getOrder("ABC123", "secret-token");
        assertEquals("DONE", order.status());
        assertEquals(new BigDecimal("28.123456"), order.to().tx().amount());
        assertEquals(new BigDecimal("9007199254.740993"), order.to().amount());
    }

    @ParameterizedTest @ValueSource(strings = {"0.5", "-0.5", "4294967296"})
    void nonzeroCodesCannotBeTruncatedOrOverflowToSuccess(String code) {
        response = response.replace("\"code\":0", "\"code\":" + code);
        assertThrows(IOException.class, () -> client.createOrder(BigDecimal.TEN, "payout"));
    }

    @Test void creationAndPollingShareTheDocumentedWeightedBudget() throws Exception {
        AtomicLong clock = new AtomicLong();
        AtomicInteger waits = new AtomicInteger();
        client = new FixedFloatClient(config, new FixedFloatRateLimiter(clock::get, nanos -> {
            assertEquals(5, calls.get()); // Four creates (200) + one status check (1).
            waits.incrementAndGet();
            clock.addAndGet(nanos);
        }));
        for (int i = 0; i < 4; i++) client.createOrder(BigDecimal.TEN, "payout");
        client.getOrder("ABC123", "secret-token");
        client.createOrder(BigDecimal.TEN, "payout"); // 201 + 50 exceeds the minute's budget.
        assertEquals(1, waits.get());
        assertEquals(TimeUnit.MINUTES.toNanos(1), clock.get());
        assertEquals(6, calls.get());
    }

    @Test void failedRequestsAlsoConsumeBudget() throws Exception {
        AtomicLong clock = new AtomicLong();
        client = new FixedFloatClient(config, new FixedFloatRateLimiter(clock::get, clock::addAndGet));
        httpStatus = 503;
        for (int i = 0; i < 5; i++) {
            assertThrows(IOException.class, () -> client.createOrder(BigDecimal.TEN, "payout"));
        }
        httpStatus = 200;
        client.getOrder("ABC123", "secret-token");
        assertEquals(TimeUnit.MINUTES.toNanos(1), clock.get());
        assertEquals(6, calls.get());
    }

    @Test void interruptingBudgetWaitDoesNotSendTheQueuedRequest() throws Exception {
        client = new FixedFloatClient(config, new FixedFloatRateLimiter(() -> 0L, nanos -> {
            throw new InterruptedException();
        }));
        for (int i = 0; i < 5; i++) client.createOrder(BigDecimal.TEN, "payout");
        try {
            assertThrows(InterruptedIOException.class, () -> client.getOrder("ABC123", "secret-token"));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(5, calls.get());
        } finally {
            Thread.interrupted();
        }
    }

    @ParameterizedTest @ValueSource(ints = {302, 307, 408, 429, 503})
    void neverReplaysFailedCreation(int status) {
        httpStatus = status;
        assertThrows(IOException.class, () -> client.createOrder(BigDecimal.TEN, "payout"));
        assertEquals(1, calls.get());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "null", "{\"code\":null}", "{\"code\":0}",
            "{\"code\":0,\"data\":{}}", "{\"code\":402,\"msg\":\"secret-token\"}", "invalid"})
    void rejectsInvalidOrErrorResponseWithoutExposingTokens(String body) {
        response = body;
        var error = assertThrows(IOException.class, () -> client.createOrder(BigDecimal.TEN, "payout"));
        assertFalse(error.getMessage().contains("secret-token"));
    }

    @Test void missingCredentialsDoNotSendRequest() {
        client = new FixedFloatClient(new FixedFloatConfig());
        assertThrows(IOException.class, () -> client.createOrder(BigDecimal.TEN, "payout"));
        assertEquals(0, calls.get());
    }

    @ParameterizedTest @ValueSource(ints = {200, 503})
    void diagnosticLogsExcludeCredentialsSignaturesAndResponseBodiesEvenAtDebug(int failureHttpStatus) throws Exception {
        config.setApiKey("sensitive-api-key");
        config.setApiSecret("sensitive-api-secret");
        Logger logger = (Logger) LoggerFactory.getLogger(FixedFloatClient.class);
        Level previousLevel = logger.getLevel();
        var captured = new ListAppender<ILoggingEvent>();
        captured.start();
        logger.addAppender(captured);
        logger.setLevel(Level.DEBUG);
        try {
            client.createOrder(BigDecimal.TEN, "payout");
            httpStatus = failureHttpStatus;
            response = "{\"code\":402,\"msg\":\"sensitive-api-key sensitive-api-secret secret-token private-response-body\"}";
            assertThrows(IOException.class, () -> client.getOrder("ABC123", "secret-token"));
            String logs = captured.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .collect(java.util.stream.Collectors.joining("\n"));
            assertTrue(logs.contains("method=create"));
            assertTrue(logs.contains("orderId=ABC123"));
            assertTrue(logs.contains("elapsedMs="));
            assertTrue(logs.contains(failureHttpStatus == 200 ? "apiCode=402" : "httpStatus=503"));
            for (String secret : new String[]{"sensitive-api-key", "sensitive-api-secret", "secret-token",
                    "private-response-body", signature}) {
                assertFalse(logs.contains(secret));
            }
        } finally {
            logger.detachAppender(captured);
            logger.setLevel(previousLevel);
            captured.stop();
        }
    }
}
