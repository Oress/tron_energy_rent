package org.ipan.nrgyrent.telegram.state.tariff;

public interface TariffSearchState {
    Integer getCurrentPage();
    String getQuery();

    TariffSearchState withCurrentPage(Integer value);
    TariffSearchState withQuery(String value);
}
