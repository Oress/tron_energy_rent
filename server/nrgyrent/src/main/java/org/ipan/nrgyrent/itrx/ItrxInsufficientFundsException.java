package org.ipan.nrgyrent.itrx;

public class ItrxInsufficientFundsException extends RuntimeException {
    public ItrxInsufficientFundsException(String message) {
        super(message);
    }

    public ItrxInsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }

}
