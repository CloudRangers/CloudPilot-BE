package com.cloudrangers.cloudpilot.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long empno;
    private final String username;

    private final String roleCode;   // 🔥 기존 role → roleCode로 명확히 이름 변경
    private final String roleName;   // 🔥 새로 추가됨

    private final Long teamId;
    private final String teamName;

    public CustomUserDetails(Long userId,
                             Long empno,
                             String username,
                             String roleCode,
                             String roleName,
                             Long teamId,
                             String teamName) {

        this.userId = userId;
        this.empno = empno;
        this.username = username;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.teamId = teamId;
        this.teamName = teamName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
