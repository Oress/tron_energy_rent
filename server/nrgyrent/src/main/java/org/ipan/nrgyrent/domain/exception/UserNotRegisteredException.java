package org.ipan.nrgyrent.domain.exception;

public class UserNotRegisteredException extends EnergyRentException {
    public UserNotRegisteredException(String message) {
        super(message);
    }

    public UserNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

}
