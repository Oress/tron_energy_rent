package org.ipan.nrgyrent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.deposit.usdt")
public class UsdtDepositConfig {
    // when disabled, incoming USDT deposits are recorded and put on hold (no Bybit exchange)
    private boolean enabled = true;
}
