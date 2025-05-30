package org.ipan.nrgyrent.telegram.state.tariff;

public interface AddTariffState {
    String getLabel();
    Long getTxType1Amount();

    AddTariffState withLabel(String value);
    AddTariffState withTxType1Amount(Long value);
}
