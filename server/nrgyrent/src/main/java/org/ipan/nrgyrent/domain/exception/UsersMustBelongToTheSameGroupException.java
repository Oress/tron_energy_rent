package org.ipan.nrgyrent.domain.exception;


public class UsersMustBelongToTheSameGroupException extends EnergyRentException {
    public UsersMustBelongToTheSameGroupException(String message) {
        super(message);
    }

    public UsersMustBelongToTheSameGroupException(String message, Throwable cause) {
        super(message, cause);
    }

}
