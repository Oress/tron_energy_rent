package org.ipan.nrgyrent.telegram.state;

public interface WithdrawParams {
    Long getAmount();
    Boolean getGroupBalance();

    WithdrawParams withAmount(Long value);
    WithdrawParams withGroupBalance(Boolean value);
}
