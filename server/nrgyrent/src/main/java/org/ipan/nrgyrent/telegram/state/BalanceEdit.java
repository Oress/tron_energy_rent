package org.ipan.nrgyrent.telegram.state;

public interface BalanceEdit {
    Long getSelectedBalanceId();

    BalanceEdit withSelectedBalanceId(Long value);
}
