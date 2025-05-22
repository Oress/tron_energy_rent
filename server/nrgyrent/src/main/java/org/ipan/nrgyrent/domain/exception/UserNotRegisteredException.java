package org.ipan.nrgyrent.domain.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class UserNotRegisteredException extends EnergyRentException {
    private List<Long> userIds;

    public UserNotRegisteredException(String message) {
        super(message);
    }

    public UserNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

}
