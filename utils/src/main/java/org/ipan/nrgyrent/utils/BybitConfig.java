package org.ipan.nrgyrent.utils;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.bybit")
public class BybitConfig {
    private String urlTestnet;
    private String urlMainnet;
    private String apiKey;
    private String apiSecret;
    private String recvWindow;
    private String usdtDepositAddress;

    @Autowired
    private ConfigurableEnvironment conf;

    public String getUrlToUse() {
        return conf.matchesProfiles("dev") ? urlTestnet : urlMainnet;
    }
}