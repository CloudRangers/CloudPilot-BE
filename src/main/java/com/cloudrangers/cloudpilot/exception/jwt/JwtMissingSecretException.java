package com.cloudrangers.cloudpilot.exception.jwt;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class JwtMissingSecretException extends CovigatorException {

  public JwtMissingSecretException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR, 10004, "JWT 서명용 시크릿 키가 설정되어 있지 않습니다. (application.properties 확인)");
  }
}