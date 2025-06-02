package org.ipan.nrgyrent.domain.exception;

import java.util.List;

import org.ipan.nrgyrent.domain.service.commands.TgUserId;

import lombok.Getter;

@Getter
public class UserNotRegisteredException extends EnergyRentException {
    private List<TgUserId> userIds;

    public UserNotRegisteredException(List<TgUserId> userIds, String message) {
        super(message);
        this.userIds = userIds;
    }

    public UserNotRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }

}
