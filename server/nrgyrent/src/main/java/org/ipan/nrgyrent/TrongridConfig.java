package org.ipan.nrgyrent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.trongrid")
public class TrongridConfig {
    private Integer qps;
    private String baseUrl;
    private String apiKey;
    private String usdtAddress;
}