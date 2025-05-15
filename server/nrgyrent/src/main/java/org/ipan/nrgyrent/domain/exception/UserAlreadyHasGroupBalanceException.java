package org.ipan.nrgyrent.domain.exception;

public class UserAlreadyHasGroupBalanceException extends EnergyRentException {
    public UserAlreadyHasGroupBalanceException(String message) {
        super(message);
    }

    public UserAlreadyHasGroupBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

}
