package com.cloudrangers.cloudpilot.exception;

public class ProvisionException extends RuntimeException {

    public ProvisionException(String message) {
        super(message);
    }

    public ProvisionException(String message, Throwable cause) {
        super(message, cause);
    }
}