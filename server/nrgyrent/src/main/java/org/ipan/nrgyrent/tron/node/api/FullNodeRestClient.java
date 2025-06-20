package org.ipan.nrgyrent.tron.node.api;

import org.ipan.nrgyrent.FullnodeConfig;
import org.ipan.nrgyrent.tron.node.api.dto.AccountResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component
@AllArgsConstructor
@Slf4j
public class FullNodeRestClient {
    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    private final ObjectMapper objectMapper;
    private final FullnodeConfig fullnodeConfig;

    private boolean tryObtainToken() throws InterruptedException {
        return true;
    }

    public AccountResource getAccountResource(String wallet) {
        AccountResource result = null;

        String responseStr = "";
        try {
            if (tryObtainToken()) {
                HttpUrl url = HttpUrl.parse(this.fullnodeConfig.getHttpApiUrl())
                    .newBuilder()
                        .addPathSegments("wallet/getaccountresource")
                        .addQueryParameter("address", wallet)
                        .addQueryParameter("visible", "true")
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Content-Type", "application/json")
                        .build();
                Response response = client.newCall(request).execute();

                responseStr = response.body().string();
                // checkReponseForFrequencyLimit(response, responseStr);
                result = objectMapper.readValue(responseStr, AccountResource.class);
            } else {
                logger.error("Could not obtain bucket4j token for getAccountInfo");
            }

        } catch (Exception e) {
            logger.error("Could not getAccountInfo response: {}", responseStr, e);
            throw new RuntimeException(e);
        }

        return result;
    }
}
