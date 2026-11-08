package org.ipan.nrgyrent.cron;

import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;

public interface UsdtExchangeStrategy {
    UsdtExchangeProvider provider();

    /** Prepare the exchange and return the USDT destination used to estimate energy. */
    String prepare(DepositTransaction deposit);

    /** Called after the energy rental completes. */
    void exchange(DepositTransaction deposit) throws InterruptedException;
}
