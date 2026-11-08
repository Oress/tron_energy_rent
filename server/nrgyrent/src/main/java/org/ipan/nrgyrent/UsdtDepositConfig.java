package org.ipan.nrgyrent;

import lombok.Data;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.deposit.usdt")
public class UsdtDepositConfig {
    public static final BigDecimal BOT_FEE_USDT = new BigDecimal("1.50");
    // When disabled, new incoming USDT deposits are recorded and put on hold.
    private boolean enabled = true;
}
