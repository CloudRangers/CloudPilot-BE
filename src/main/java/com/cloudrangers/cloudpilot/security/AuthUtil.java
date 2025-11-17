package com.cloudrangers.cloudpilot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public class AuthUtil {

    /** ✅ 현재 로그인한 사용자의 사번(empno) */
    public static String getCurrentEmpno() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        return authentication.getName(); // empno
    }

    /** ✅ 현재 로그인한 사용자의 역할(role) */
    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) return null;
        if (!(auth.getDetails() instanceof Map)) return null;
        return (String) ((Map<?, ?>) auth.getDetails()).get("role");
    }

    /** ✅ 현재 로그인한 사용자의 소속 팀(team) */
    public static String getCurrentTeam() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) return null;
        if (!(auth.getDetails() instanceof Map)) return null;
        return (String) ((Map<?, ?>) auth.getDetails()).get("team");
    }

    /** ✅ 로그인 여부 확인 */
    public static boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }
}
