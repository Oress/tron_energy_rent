// Reusable Bybit V5 request-signing helpers for the JetBrains HTTP Client.
//
// Mirrors org.ipan.nrgyrent.bybit.BybitRestClient (genGetSign / genPostSign):
//   sign = HMAC_SHA256(apiSecret, timestamp + apiKey + recvWindow + payload)  (hex)
// where payload is the query string (GET) or the raw JSON body (POST).
//
// Each helper also publishes the request variables consumed by the headers:
//   {{apiKey}}, {{recvWindow}}, {{timestamp}}, {{sign}}
//
// Credentials are read from the active HTTP-client environment:
//   bybitApiKey, bybitApiSecret, bybitRecvWindow  (see http-client.private.env.json)

function sign(request, crypto, payloadTail) {
    const apiKey = request.environment.get("bybitApiKey");
    const apiSecret = request.environment.get("bybitApiSecret");
    const recvWindow = request.environment.get("bybitRecvWindow") || "5000";
    const timestamp = Date.now().toString();

    const message = timestamp + apiKey + recvWindow + (payloadTail || "");
    const signature = crypto.hmac.sha256()
        .withTextSecret(apiSecret)
        .updateWithText(message)
        .digest()
        .toHex();

    request.variables.set("apiKey", apiKey);
    request.variables.set("recvWindow", recvWindow);
    request.variables.set("timestamp", timestamp);
    request.variables.set("sign", signature);
    return signature;
}

// GET: payload is the query string exactly as appended to the URL (no leading '?').
export function signGet(request, crypto, queryString) {
    return sign(request, crypto, queryString);
}

// POST: payload is the raw JSON body string. Publishes {{requestBody}} so the body
// sent over the wire is byte-for-byte identical to what was signed.
export function signPost(request, crypto, bodyObject) {
    const body = JSON.stringify(bodyObject);
    request.variables.set("requestBody", body);
    return sign(request, crypto, body);
}

// RFC-4122 v4 UUID (used for transferId, like UUID.randomUUID() in Java).
export function uuid() {
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === "x" ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}
