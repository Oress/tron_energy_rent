package org.ipan.nrgyrent.telegram.state.tariff;

public interface TariffEdit {
    Long getSelectedTariffId();

    TariffEdit withSelectedTariffId(Long value);
}
