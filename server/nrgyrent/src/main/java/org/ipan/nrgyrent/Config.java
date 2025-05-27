package org.ipan.nrgyrent;

import org.ipan.nrgyrent.domain.service.CollectionWalletService;
import org.ipan.nrgyrent.trongrid.ApiClient;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableRetry
@EnableAsync
@EnableAspectJAutoProxy
@Slf4j
public class Config {
    @Value("${app.trongrid.base-url}")
    String baseUrl = "https://nile.trongrid.io";
    @Value("${app.trongrid.api-key}")
    String apiKey = "https://nile.trongrid.io";

    @Autowired
    CollectionWalletService collectionWalletService;

    @Bean
    public ApiClient apiClient() {
        ApiClient defaultClient = new ApiClient();
        logger.info("Base URL: {}", baseUrl);
        defaultClient.setBasePath(baseUrl);
        defaultClient.addDefaultHeader("TRON-PRO-API-KEY", apiKey);
        return defaultClient;
    }

    @Bean
    public AccountApi accountApi() {
        return new AccountApi(apiClient());
    }

    @PostConstruct
    public void initializeApplication() {
        collectionWalletService.ensureWalletsAreCreated();
    }

/*    @Bean
//    @Profile("development")
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
//                .addDefaultScripts()
                .build();
    }*/
}
