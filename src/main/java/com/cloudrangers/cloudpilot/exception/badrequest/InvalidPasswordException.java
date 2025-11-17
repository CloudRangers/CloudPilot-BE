package com.cloudrangers.cloudpilot.exception.badrequest;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends CovigatorException {

    public InvalidPasswordException() {
        super(HttpStatus.BAD_REQUEST, 10001, "비밀번호가 올바르지 않습니다.");
    }
}