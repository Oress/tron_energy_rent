package org.ipan.nrgyrent;

import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.telegram.TelegramMsgScopeBpp;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableRetry
@EnableAsync
@EnableAspectJAutoProxy
@Slf4j
public class BasicConfig {
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
}
