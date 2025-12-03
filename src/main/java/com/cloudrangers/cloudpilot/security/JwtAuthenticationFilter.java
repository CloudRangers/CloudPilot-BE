package com.cloudrangers.cloudpilot.security;

import com.cloudrangers.cloudpilot.exception.jwt.JwtExpiredException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtInvalidException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        log.info(">>> [JWT-FILTER] Processing URI: {}", uri);

        // 로그인/리프레시/로그아웃은 토큰 검증 스킵
        if (uri.startsWith("/auth/login") ||
                uri.startsWith("/auth/refresh") ||
                uri.startsWith("/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더 사용 안함 — 쿠키 ONLY
        String token = resolveToken(request);
        log.info("🍪 [JWT-FILTER] Extracted Token = {}", token);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 블랙리스트 확인
        if (redisTemplate.hasKey("BLACKLIST:" + token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token is blacklisted");
            return;
        }

        try {
            log.info("🟢 [JWT-FILTER] Validating Token = {}", token);

            if (jwtProvider.validateToken(token)) {

                Claims claims = jwtProvider.parseClaims(token);

                Long userId   = claims.get("userId", Long.class);
                Long empno    = claims.get("empno", Long.class);
                String username = claims.get("username", String.class);

                String roleCode = claims.get("role", String.class);
                String roleName = claims.get("roleName", String.class);

                Long teamId   = claims.get("teamId", Long.class);
                String teamName = claims.get("teamName", String.class);

                CustomUserDetails principal = new CustomUserDetails(
                        userId,
                        empno,
                        username,
                        roleCode,
                        roleName,
                        teamId,
                        teamName
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtExpiredException e) {
            log.warn("⚠️ [JWT-FILTER] Token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Access token expired");
            return;

        } catch (JwtInvalidException e) {
            log.error("❌ [JWT-FILTER] Invalid Token Reason = {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token");
            return;

        } catch (Exception e) {
            log.error("🔥 [JWT-FILTER] Unexpected error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid authorization");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더 완전 제거
     * access_token 쿠키 ONLY 사용
     */
    private String resolveToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
