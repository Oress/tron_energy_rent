package org.ipan.nrgyrent.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.NrgConfigs;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.NrgConfigsRepository;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class NrgConfigsService {
    private final NrgConfigsRepository nrgConfigsRepository;
    private final BalanceRepo balanceRepo;

    @Transactional(readOnly = true)
    public EnergyProviderName readCurrentProviderConfig() {
        NrgConfigs byId = nrgConfigsRepository.findById(AppConstants.CONFIG_ENERGY_PROVIDER).get();
        return EnergyProviderName.valueOf(byId.getValue());
    }

    @Transactional
    public void updateCurrentProviderConfig(EnergyProviderName value) {
        logger.info("Updating current energy provider config to: {}", value);
        NrgConfigs byId = nrgConfigsRepository.findById(AppConstants.CONFIG_ENERGY_PROVIDER).get();
        byId.setValue(value.name());
        balanceRepo.updateAllBalancesForEnergyProvider(value);
    }

    @Transactional(readOnly = true)
    public EnergyProviderName readCurrentAutoProviderConfig() {
        NrgConfigs byId = nrgConfigsRepository.findById(AppConstants.CONFIG_AUTO_ENERGY_PROVIDER).get();
        return EnergyProviderName.valueOf(byId.getValue());
    }

    @Transactional
    public void updateCurrentAutoProviderConfig(EnergyProviderName value) {
        logger.info("Updating current AUTO energy provider config to: {}", value);
        NrgConfigs byId = nrgConfigsRepository.findById(AppConstants.CONFIG_AUTO_ENERGY_PROVIDER).get();
        byId.setValue(value.name());
        balanceRepo.updateAllBalancesForAutoEnergyProvider(value);
    }

}
