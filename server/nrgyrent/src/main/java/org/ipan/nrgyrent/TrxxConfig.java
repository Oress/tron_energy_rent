package org.ipan.nrgyrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.trxx")
public class TrxxConfig extends ItrxConfig {
}