package org.ipan.nrgyrent.cron;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.GetCoinBalanceResp;
import org.ipan.nrgyrent.domain.model.BybitBalance;
import org.ipan.nrgyrent.domain.model.repository.BybitBalanceRepository;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Slf4j
@AllArgsConstructor
public class BybitBalanceMonitorJob {

    private final BybitBalanceRepository bybitBalanceRepository;
    private final BybitRestClient bybitRestClient;

    @Transactional
    public void monitor() {
        BybitBalance balance = bybitBalanceRepository.findById("TRX").orElse(null);

        GetCoinBalanceResp trxUnified = bybitRestClient.getCoinBalance("TRX", "UNIFIED");
        GetCoinBalanceResp trxFund = bybitRestClient.getCoinBalance("TRX", "FUND");

        if (balance == null) {
            balance = new BybitBalance();
            balance.setCoin("TRX");
            balance = bybitBalanceRepository.save(balance);
        }
        BigDecimal result = BigDecimal.ZERO;
        if (trxUnified.getResult() != null && trxUnified.getResult().getBalance() != null && trxUnified.getResult().getBalance().getWalletBalance() != null) {
            result = result.add(trxUnified.getResult().getBalance().getWalletBalance());
        }

        if (trxFund.getResult() != null && trxFund.getResult().getBalance() != null && trxFund.getResult().getBalance().getWalletBalance() != null) {
            result = result.add(trxFund.getResult().getBalance().getWalletBalance());
        }

        balance.setBalance(result);
    }
}
