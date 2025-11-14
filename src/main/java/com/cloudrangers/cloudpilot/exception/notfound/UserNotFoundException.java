package com.cloudrangers.cloudpilot.exception.notfound;

import com.cloudrangers.cloudpilot.exception.CovigatorException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CovigatorException {

    public UserNotFoundException(Long empno) {
        super(HttpStatus.NOT_FOUND, 10005, "사번 " + empno + "에 해당하는 사용자를 찾을 수 없습니다.");
    }

    public UserNotFoundException(String email) {
        super(HttpStatus.NOT_FOUND, 10005, "이메일 " + email + "에 해당하는 사용자를 찾을 수 없습니다.");
    }
}
