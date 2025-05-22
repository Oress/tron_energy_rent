package org.ipan.nrgyrent.tron;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

import org.ipan.nrgyrent.tron.trongrid.TrongridRestClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
public class TronTransactionHelper {
    private final TrongridRestClient trongridRestClient;

    public String performTransferTransaction(String fromAddress, String toAddress, Long amountToTransfer, Function<String, String> signFunction) {
        logger.info("Performing transfer transaction from {} to {} with amount {}", fromAddress, toAddress, amountToTransfer);
        TreeMap<String, Object> responseProps = trongridRestClient.createTransaction(
                fromAddress,
                toAddress,
                amountToTransfer);

        // TODO: handle error response responseProps["Error"] != null
        String txId = (String) responseProps.get("txID");
        String signature = signFunction.apply(txId);

        responseProps.put("signature", List.of(signature));
        TreeMap<String, Object> broadcastResult = trongridRestClient.broadcastTransaction(responseProps);
        String code = (String) broadcastResult.get("code");
        if (code == null || code.isEmpty()) {
            logger.info("Transaction successful: {}", broadcastResult.get("txid"));
        } else {
            logger.error("Transaction failed: {}", broadcastResult.get("message"));
        }
        return txId;
    }
}
