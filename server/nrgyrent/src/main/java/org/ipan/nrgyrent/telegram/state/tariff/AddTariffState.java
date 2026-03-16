package org.ipan.nrgyrent.telegram.state.tariff;

public interface AddTariffState {
    String getLabel();
    Long getTxType1Amount();
    Long getTxType2Amount();
    Long getAmlCheckPriceSun();

    AddTariffState withLabel(String value);
    AddTariffState withTxType1Amount(Long value);
    AddTariffState withTxType2Amount(Long value);
    AddTariffState withAmlCheckPriceSun(Long value);
}
