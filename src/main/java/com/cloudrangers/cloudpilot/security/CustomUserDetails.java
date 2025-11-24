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
    private final String role;
    private final Long teamId;
    private final String teamName;

    public CustomUserDetails(Long userId,
                             Long empno,
                             String username,
                             String role,
                             Long teamId,
                             String teamName) {
        this.userId = userId;
        this.empno = empno;
        this.username = username;
        this.role = role;
        this.teamId = teamId;
        this.teamName = teamName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
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
