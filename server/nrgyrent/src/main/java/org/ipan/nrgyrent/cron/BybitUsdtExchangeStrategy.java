package org.ipan.nrgyrent.cron;

import lombok.RequiredArgsConstructor;
import org.ipan.nrgyrent.BybitConfig;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.DepositData;
import org.ipan.nrgyrent.domain.model.DepositStatus;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BybitUsdtExchangeStrategy implements UsdtExchangeStrategy {
    private final UsdtDepositHelper helper;
    private final BybitConfig config;
    private final BybitRestClient client;
    private final ConfigurableEnvironment environment;

    @Override
    public UsdtExchangeProvider provider() {
        return UsdtExchangeProvider.BYBIT;
    }

    @Override
    public String prepare(DepositTransaction deposit) {
        return config.getUsdtDepositAddress();
    }

    @Override
    public void exchange(DepositTransaction deposit) throws InterruptedException {
        String txId = helper.transferUsdtToBybit(deposit);
        if (deposit.getStatus() == DepositStatus.USDT_TRANSFERRED_TO_BYBIT_FAILED) {
            return;
        }
        if (!environment.matchesProfiles("dev")) {
            boolean confirmed = false;
            for (int i = 0; i < 70; i++) {
                DepositData data = client.getUsdtDeposits(txId);
                if (data != null && data.getStatus() != null && data.getConfirmationsInt() > 30
                        && (data.getStatus() == 10012 || data.getStatus() == 3)) {
                    confirmed = true;
                    break;
                }
                Thread.sleep(3000);
            }
            if (!confirmed) {
                throw new IllegalStateException("Bybit deposit confirmation timed out for deposit " + deposit.getId());
            }
        }
        helper.placeBuyOrderUsdtToTrx(deposit);
    }
}
