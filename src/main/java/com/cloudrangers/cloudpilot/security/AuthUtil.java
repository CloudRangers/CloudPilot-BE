package com.cloudrangers.cloudpilot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    /** 내부에서 principal 가져오는 공통 메서드 */
    private static CustomUserDetails getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (!(auth.getPrincipal() instanceof CustomUserDetails)) return null;
        return (CustomUserDetails) auth.getPrincipal();
    }

    /** 🔥 현재 로그인한 사용자 userId */
    public static Long getUserId() {
        CustomUserDetails p = getPrincipal();
        return p != null ? p.getUserId() : null;
    }

    /** 🔥 현재 로그인한 사용자 empno */
    public static String getEmpno() {
        CustomUserDetails p = getPrincipal();
        return p != null ? p.getEmpno() : null;
    }

    /** 🔥 현재 로그인한 사용자 역할 (HEAD / LEADER / MEMBER) */
    public static String getRole() {
        CustomUserDetails p = getPrincipal();
        return p != null ? p.getRole() : null;
    }

    /** 🔥 현재 로그인한 사용자 teamId */
    public static Long getTeamId() {
        CustomUserDetails p = getPrincipal();
        return p != null ? p.getTeamId() : null;
    }

    /** 로그인 여부 */
    public static boolean isAuthenticated() {
        return getPrincipal() != null;
    }
}