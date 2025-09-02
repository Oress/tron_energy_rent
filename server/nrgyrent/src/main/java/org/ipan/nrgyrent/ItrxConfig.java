package org.ipan.nrgyrent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.itrx")
public class ItrxConfig {
    private String baseUrl;
    private String callbackUrl;
    private String key;
    private String secret;
    private String autoDelegationThreshold;
}