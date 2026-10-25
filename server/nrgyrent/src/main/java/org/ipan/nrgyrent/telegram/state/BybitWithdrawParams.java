package org.ipan.nrgyrent.telegram.state;

import java.math.BigDecimal;

public record BybitWithdrawParams(Long walletId, String address, BigDecimal amount, String requestId) {
}
