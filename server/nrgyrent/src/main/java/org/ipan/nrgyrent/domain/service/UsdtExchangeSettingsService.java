package org.ipan.nrgyrent.domain.service;

import lombok.RequiredArgsConstructor;
import org.ipan.nrgyrent.domain.model.NrgConfigs;
import org.ipan.nrgyrent.domain.model.UsdtExchangeProvider;
import org.ipan.nrgyrent.domain.model.repository.NrgConfigsRepository;
import org.ipan.nrgyrent.fixedfloat.FixedFloatConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsdtExchangeSettingsService {
    public static final String CONFIG_KEY = "USDT_EXCHANGE_PROVIDER";
    private final NrgConfigsRepository configs;
    private final FixedFloatConfig fixedFloat;

    @Transactional(readOnly = true)
    public UsdtExchangeProvider currentProvider() {
        return configs.findById(CONFIG_KEY).map(c -> UsdtExchangeProvider.valueOf(c.getValue()))
                .orElseThrow(() -> new IllegalStateException("Missing USDT exchange provider setting; apply database migrations"));
    }

    public boolean isFixedFloatConfigured() {
        return fixedFloat.getApiKey() != null && !fixedFloat.getApiKey().isBlank()
                && fixedFloat.getApiSecret() != null && !fixedFloat.getApiSecret().isBlank();
    }

    @Transactional
    public void updateProvider(UsdtExchangeProvider provider) {
        if (provider == UsdtExchangeProvider.FIXEDFLOAT && !isFixedFloatConfigured()) {
            throw new IllegalStateException("FixedFloat API credentials are not configured");
        }
        NrgConfigs config = configs.findById(CONFIG_KEY).orElseGet(NrgConfigs::new);
        config.setId(CONFIG_KEY);
        config.setValue(provider.name());
        configs.save(config);
    }
}
