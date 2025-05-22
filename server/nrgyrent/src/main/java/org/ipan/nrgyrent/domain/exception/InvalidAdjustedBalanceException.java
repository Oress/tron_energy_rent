package org.ipan.nrgyrent.domain.exception;

public class InvalidAdjustedBalanceException extends EnergyRentException {
    public InvalidAdjustedBalanceException(String message) {
        super(message);
    }

    public InvalidAdjustedBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
