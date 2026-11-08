package org.ipan.nrgyrent.domain.service;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.repository.NrgConfigsRepository;
import org.ipan.nrgyrent.fixedfloat.FixedFloatConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsdtExchangeSettingsServiceTest {
    private final NrgConfigsRepository repo = mock(NrgConfigsRepository.class);
    private final FixedFloatConfig ff = new FixedFloatConfig();
    private final UsdtExchangeSettingsService service = new UsdtExchangeSettingsService(repo, ff);

    @Test void readsInitialProviderFromDatabase() {
        NrgConfigs config = new NrgConfigs();
        config.setId(UsdtExchangeSettingsService.CONFIG_KEY);
        config.setValue("BYBIT");
        when(repo.findById(config.getId())).thenReturn(Optional.of(config));
        assertEquals(UsdtExchangeProvider.BYBIT, service.currentProvider());
    }

    @Test void missingDatabaseSettingDoesNotSilentlyChooseProvider() {
        assertThrows(IllegalStateException.class, service::currentProvider);
    }

    @Test void persistsAdminSelectionAndReadsItAfterRestart() {
        ff.setApiKey("key"); ff.setApiSecret("secret");
        service.updateProvider(UsdtExchangeProvider.FIXEDFLOAT);
        var captor = ArgumentCaptor.forClass(NrgConfigs.class);
        verify(repo).save(captor.capture());
        NrgConfigs saved = captor.getValue();
        assertEquals(UsdtExchangeSettingsService.CONFIG_KEY, saved.getId());
        assertEquals("FIXEDFLOAT", saved.getValue());
        when(repo.findById(saved.getId())).thenReturn(Optional.of(saved));
        var restarted = new UsdtExchangeSettingsService(repo, ff);
        assertEquals(UsdtExchangeProvider.FIXEDFLOAT, restarted.currentProvider());
    }

    @Test void rejectsSelectionWithoutCredentials() {
        assertThrows(IllegalStateException.class, () -> service.updateProvider(UsdtExchangeProvider.FIXEDFLOAT));
        verify(repo, never()).save(any());
    }

    @Test void canAlwaysSwitchBackToBybit() {
        service.updateProvider(UsdtExchangeProvider.BYBIT);
        verify(repo).save(any());
    }
}
