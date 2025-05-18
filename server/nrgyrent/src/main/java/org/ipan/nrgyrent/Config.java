package org.ipan.nrgyrent;

import org.ipan.nrgyrent.domain.service.CollectionWalletService;
import org.ipan.nrgyrent.trongrid.ApiClient;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.ipan.nrgyrent.trongrid.api.TransactionApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import jakarta.annotation.PostConstruct;

@Configuration
@EnableRetry
public class Config {
    @Value("${app.trongrid.base-url}")
    String baseUrl = "https://nile.trongrid.io";

    @Autowired
    CollectionWalletService collectionWalletService;

    @Bean
    public ApiClient apiClient() {
        ApiClient defaultClient = new ApiClient();
        defaultClient.setBasePath(baseUrl);
        // defaultClient.addDefaultHeader("nrgyrent/0.1");
        return defaultClient;
    }

    @Bean
    public AccountApi accountApi() {
        return new AccountApi(apiClient());
    }

    @Bean
    public TransactionApi transactionApi() {
        return new TransactionApi(apiClient());
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
