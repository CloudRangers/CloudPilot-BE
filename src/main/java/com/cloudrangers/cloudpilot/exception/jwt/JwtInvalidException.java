package com.cloudrangers.cloudpilot.exception.jwt;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class JwtInvalidException extends CovigatorException {

    public JwtInvalidException() {
        super(HttpStatus.UNAUTHORIZED, 10002, "유효하지 않은 JWT 토큰입니다.");
    }
}
