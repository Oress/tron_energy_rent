package org.ipan.nrgyrent.itrx.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.itrx.ItrxService;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;

@RestController
@Slf4j
public class TrxxCallbackController {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    @Value("${app.trxx.key}")
    String apiKey;
    @Value("${app.trxx.secret}")
    String apiSecret;

    @Autowired
    ItrxService itrxService;

    @PostMapping("/api/trxx/callback")
    public ResponseEntity<?> handleCallback(@RequestHeader("TIMESTAMP") String timestamp,
                                            @RequestHeader("SIGNATURE") String signature,
                                            @RequestBody Map<String, Object> requestBody) throws Exception {
        logger.info("TRXX. Received callback: {}", requestBody);

        TreeMap<String, Object> sortedBody = new TreeMap<>(requestBody);
        String jsonData = gson.toJson(sortedBody);

        String message = timestamp + "&" + jsonData;

        String expectedSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, apiSecret).hmacHex(message);

        if (!signature.equals(expectedSignature)) {
            logger.error("TRXX. Invalid signature: expected {}, got {}", expectedSignature, signature);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        OrderCallbackRequest placeOrderResponse = gson.fromJson(jsonData, OrderCallbackRequest.class);

        itrxService.processCallback(placeOrderResponse, EnergyProviderName.TRXX);

        return ResponseEntity.ok().build();
    }
}
