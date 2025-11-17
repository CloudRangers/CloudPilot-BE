package com.cloudrangers.cloudpilot.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotNull(message = "사번(empno)은 필수 입력값입니다.")
    private Long empno;

    @NotNull(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 4, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력해주세요.")
    private String password;
}