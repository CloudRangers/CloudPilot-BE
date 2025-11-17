package com.cloudrangers.cloudpilot.exception.badrequest;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends CovigatorException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, 10006, message);
    }
}
