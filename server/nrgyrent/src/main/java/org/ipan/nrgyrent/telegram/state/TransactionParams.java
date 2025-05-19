package org.ipan.nrgyrent.telegram.state;

public interface TransactionParams {
    Integer getEnergyAmount();
    Boolean getGroupBalance();

    TransactionParams withEnergyAmount(Integer value);
    TransactionParams withGroupBalance(Boolean value);
}
