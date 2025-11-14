package com.cloudrangers.cloudpilot.security;

import com.cloudrangers.cloudpilot.exception.jwt.JwtInvalidException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // ✅ 블랙리스트 차단
        if (redisTemplate.hasKey("BLACKLIST:" + token)) {
            log.warn("⛔ 블랙리스트 토큰 접근 시도: {}", token);
            throw new JwtInvalidException();
        }

        // ✅ 토큰 검증 및 Claims 추출
        if (jwtProvider.validateToken(token)) {
            Claims claims = jwtProvider.getClaims(token);
            String empno = claims.getSubject();
            String role = (String) claims.get("role");
            String team = (String) claims.get("team");

            log.debug("🎟️ JWT 인증 성공: empno={}, role={}, team={}", empno, role, team);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            new User(empno, "", Collections.emptyList()),
                            null,
                            Collections.emptyList()
                    );

            // ✅ role/team 정보를 함께 저장 — AuthUtil에서 꺼낼 수 있게
            authentication.setDetails(Map.of(
                    "role", role,
                    "team", team,
                    "ip", request.getRemoteAddr()  // optional
            ));

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
