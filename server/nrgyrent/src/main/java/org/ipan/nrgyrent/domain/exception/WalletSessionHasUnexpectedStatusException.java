package org.ipan.nrgyrent.domain.exception;

public class WalletSessionHasUnexpectedStatusException extends EnergyRentException {
    public WalletSessionHasUnexpectedStatusException(String message) {
        super(message);
    }

    public WalletSessionHasUnexpectedStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
