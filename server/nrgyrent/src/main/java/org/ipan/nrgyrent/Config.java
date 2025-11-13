package org.ipan.nrgyrent;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.cron.ItrxAlertConfig;
import org.ipan.nrgyrent.cron.ItrxBalanceMonitorCronJob;
import org.ipan.nrgyrent.cron.NettsBalanceMonitorCronJob;
import org.ipan.nrgyrent.domain.model.Alert;
import org.ipan.nrgyrent.domain.model.repository.AlertRepo;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.ItrxBalanceRepository;
import org.ipan.nrgyrent.domain.service.AlertService;
import org.ipan.nrgyrent.domain.service.CollectionWalletService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.itrx.RestClient;
import org.ipan.nrgyrent.netts.NettsRestClient;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
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
    NettsRestClient nettsRestClient;

    @Autowired
    ItrxConfig itrxConfig;

    @Autowired
    TrxxConfig trxxConfig;

    @Autowired
    private TelegramMessages telegramMessages;

    @Autowired
    private TelegramState telegramState;

    @Autowired
    private AppUserRepo userRepo;

    @Autowired
    private AlertRepo alertRepo;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ItrxBalanceRepository itrxBalanceRepository;

    @Value("${app.alerts.itrx-balance.threashold:800000000}")
    private Long balanceThreshold;


    @PostConstruct
    public void initializeApplication() {
        collectionWalletService.ensureWalletsAreCreated();
    }

    @Bean
    public RestClient itrxRestClient() {
        return new RestClient(itrxConfig);
    }

    @Bean(value = AppConstants.TRXX_REST_CLIENT)
    public RestClient trxxRestClient() {
        return new RestClient(trxxConfig);
    }

    @Bean
    public ItrxBalanceMonitorCronJob itrxBalanceMonitorCronJob() {
        return new ItrxBalanceMonitorCronJob(
                new ItrxAlertConfig(AppConstants.ITRX, Alert.ITRX_BALANCE_LOW),
                itrxRestClient(),
                telegramMessages,
                telegramState,
                userRepo,
                alertRepo,
                alertService,
                itrxBalanceRepository,
                balanceThreshold
        );
    }

    @Bean
    public NettsBalanceMonitorCronJob nettsBalanceMonitorCronJob() {
        return new NettsBalanceMonitorCronJob(
                new ItrxAlertConfig("NETTS.IO", Alert.NETTS_BALANCE_LOW),
                nettsRestClient,
                telegramMessages,
                telegramState,
                userRepo,
                alertRepo,
                alertService,
                itrxBalanceRepository,
                balanceThreshold
        );
    }

    @Bean(value = AppConstants.TRXX_MONITOR_JOB)
    public ItrxBalanceMonitorCronJob trxxBalanceMonitorCronJob() {
        return new ItrxBalanceMonitorCronJob(
                new ItrxAlertConfig(AppConstants.TRXX, Alert.TRXX_BALANCE_LOW),
                trxxRestClient(),
                telegramMessages,
                telegramState,
                userRepo,
                alertRepo,
                alertService,
                itrxBalanceRepository,
                balanceThreshold
        );
    }
}
