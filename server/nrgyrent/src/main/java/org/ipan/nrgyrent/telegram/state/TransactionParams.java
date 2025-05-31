package org.ipan.nrgyrent.telegram.state;

public interface TransactionParams {
    Integer getNumberOfTransactions();
    Integer getEnergyAmount();
    Boolean getGroupBalance();

    TransactionParams withNumberOfTransactions(Integer value);
    TransactionParams withEnergyAmount(Integer value);
    TransactionParams withGroupBalance(Boolean value);
}
