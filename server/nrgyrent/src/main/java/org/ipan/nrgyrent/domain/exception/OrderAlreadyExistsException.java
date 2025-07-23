package org.ipan.nrgyrent.domain.exception;

public class OrderAlreadyExistsException extends EnergyRentException {
    public OrderAlreadyExistsException(String message) {
        super(message);
    }

    public OrderAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
