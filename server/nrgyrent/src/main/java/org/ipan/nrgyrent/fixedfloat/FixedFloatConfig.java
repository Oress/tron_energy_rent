package org.ipan.nrgyrent.fixedfloat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.fixedfloat")
public class FixedFloatConfig {
    private String baseUrl = "https://ff.io/api/v2";
    private String apiKey;
    private String apiSecret;
    private String refcode;
}
