package org.ipan.nrgyrent.domain.exception;

public class UserIsDisabledException extends EnergyRentException {
    public UserIsDisabledException(String message) {
        super(message);
    }

    public UserIsDisabledException(String message, Throwable cause) {
        super(message, cause);
    }

}
