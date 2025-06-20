package org.ipan.nrgyrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.fullnode")
public class FullnodeConfig {
    private String httpApiUrl;
    private String zeroMqUrl;
}