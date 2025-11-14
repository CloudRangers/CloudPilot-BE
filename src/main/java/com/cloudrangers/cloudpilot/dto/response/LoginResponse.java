package com.cloudrangers.cloudpilot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String role;
    private String team;
    private String username;
}
