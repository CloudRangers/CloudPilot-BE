package com.cloudrangers.cloudpilot.common.exception;

public class CatalogNotFoundException extends RuntimeException {
    private final String errorCode;
    public CatalogNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
