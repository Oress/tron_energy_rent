package org.ipan.nrgyrent;

import org.ipan.nrgyrent.trongrid.ApiClient;
import org.ipan.nrgyrent.trongrid.api.AccountApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class Config {
    @Value("${app.trongrid.base-url}")
    String baseUrl = "https://nile.trongrid.io";

    @Bean
    public ApiClient apiClient() {
        ApiClient defaultClient = new ApiClient();
        defaultClient.setBasePath(baseUrl);
        return defaultClient;
    }

    @Bean
    public AccountApi accountApi() {
        return new AccountApi(apiClient());
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
