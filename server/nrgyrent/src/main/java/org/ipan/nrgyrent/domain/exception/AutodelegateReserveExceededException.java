package org.ipan.nrgyrent.domain.exception;

import lombok.Getter;

@Getter
public class AutodelegateReserveExceededException extends EnergyRentException {
    private Long minimumAmount;

    public AutodelegateReserveExceededException(String message, Long minimumAmount) {
        super(message);
        this.minimumAmount = minimumAmount;
    }

    public AutodelegateReserveExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
