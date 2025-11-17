package com.cloudrangers.cloudpilot.common.exception;

public class CatalogConflictException extends RuntimeException {
    private final String errorCode;
    public CatalogConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
