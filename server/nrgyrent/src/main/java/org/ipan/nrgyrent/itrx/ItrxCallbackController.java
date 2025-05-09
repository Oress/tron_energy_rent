package org.ipan.nrgyrent.itrx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ipan.nrgyrent.itrx.dto.OrderCallbackRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

@RestController
public class ItrxCallbackController {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @Value("${app.itrx.key}")
    String apiKey;
    @Value("${app.itrx.secret}")
    String apiSecret;

    @PostMapping("/api/itrx/callback")
    @Transactional
    public ResponseEntity<?> handleCallback(@RequestHeader("TIMESTAMP") String timestamp,
                                            @RequestHeader("SIGNATURE") String signature,
                                            @RequestBody Map<String, Object> requestBody) throws Exception {
        System.out.println("Received callback: " + requestBody);

        TreeMap<String, Object> sortedBody = new TreeMap<>(requestBody);
        String jsonData = gson.toJson(sortedBody);

        String message = timestamp + "&" + jsonData;

        String expectedSignature = Utils.encodeHmacSHA256(message, apiSecret);

        if (!signature.equals(expectedSignature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // Deserialize the callback data and process it ...

        return ResponseEntity.ok().build();
    }
}
