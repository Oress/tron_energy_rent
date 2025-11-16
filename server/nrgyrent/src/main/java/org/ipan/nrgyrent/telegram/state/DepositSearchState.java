package org.ipan.nrgyrent.telegram.state;

public interface DepositSearchState {
    Integer getCurrentPage();

    DepositSearchState withCurrentPage(Integer value);
}
