package com.cloudrangers.cloudpilot.exception.badrequest;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

/**
 * JWT 토큰이 유효하지 않거나 Authorization 헤더 형식이 잘못된 경우 발생하는 예외
 */
public class InvalidTokenException extends CovigatorException {

    public InvalidTokenException() {
        super(HttpStatus.BAD_REQUEST, 10002, "유효하지 않은 토큰입니다.");
    }

    public InvalidTokenException(String message) {
        super(HttpStatus.BAD_REQUEST, 10002, message);
    }
}
