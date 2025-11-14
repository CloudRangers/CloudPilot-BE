package com.cloudrangers.cloudpilot.exception.jwt;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class JwtExpiredException extends CovigatorException {

    public JwtExpiredException() {
        super(HttpStatus.UNAUTHORIZED, 10003, "만료된 JWT 토큰입니다.");
    }
}
