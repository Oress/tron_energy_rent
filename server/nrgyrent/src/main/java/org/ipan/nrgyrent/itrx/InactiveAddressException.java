package org.ipan.nrgyrent.itrx;

public class InactiveAddressException extends RuntimeException {
    public InactiveAddressException(String message) {
        super(message);
    }

    public InactiveAddressException(String message, Throwable cause) {
        super(message, cause);
    }

}
