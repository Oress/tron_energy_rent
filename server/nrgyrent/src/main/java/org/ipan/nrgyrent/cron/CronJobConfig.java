package org.ipan.nrgyrent.cron;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CronJobConfig {
    public static final String TRON_TRANSACTION_EXECUTOR = "tronTransactionExecutor";

    @Bean(name = TRON_TRANSACTION_EXECUTOR)
    public Executor asyncExecutor() {
        return Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("tronTransactionExecutor-" + thread.threadId());
            return thread;
        });
    }
}
