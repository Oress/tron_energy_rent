package org.ipan.nrgyrent;

import org.ipan.nrgyrent.domain.service.CollectionWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.telegram.TelegramMsgScopeBpp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ResourceBundleMessageSource;
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

    @Autowired
    ItrxConfig itrxConfig;

    @Autowired
    TrxxConfig trxxConfig;


    @PostConstruct
    public void initializeApplication() {
        collectionWalletService.ensureWalletsAreCreated();
    }

    @Bean
    public static TelegramMsgScopeBpp beanFactoryPostProcessor() {
        return new TelegramMsgScopeBpp();
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    @Bean
    public RestClient itrxRestClient() {
        return new RestClient(itrxConfig);
    }

    @Bean(value = AppConstants.TRXX_REST_CLIENT)
    public RestClient trxxRestClient() {
        return new RestClient(trxxConfig);
    }
}
