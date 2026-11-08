package org.ipan.nrgyrent.fixedfloat;

import java.math.BigDecimal;

// Do not log the order response: the token grants access to the order.
public record FixedFloatOrder(String id, String token, String type, String status,
                              Currency from, Currency to) {
    public record Currency(String code, String network, BigDecimal amount, String address, Transaction tx) {}
    public record Transaction(String id, BigDecimal amount) {}

    @Override
    public String toString() {
        return "FixedFloatOrder[id=" + id + ", status=" + status + "]";
    }
}
