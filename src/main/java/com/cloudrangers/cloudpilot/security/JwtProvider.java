package com.cloudrangers.cloudpilot.security;

import com.cloudrangers.cloudpilot.exception.jwt.JwtExpiredException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtInvalidException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtMissingSecretException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    // 30분
    private static final long ACCESS_TOKEN_EXP_MS  = 1000L * 60 * 30;

    // 14일
    private static final long REFRESH_TOKEN_EXP_MS = 1000L * 60 * 60 * 24 * 14;

    private SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new JwtMissingSecretException();
        }
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateAccessToken(String empno, Map<String, Object> claims) {
        return Jwts.builder()
                .subject(empno)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXP_MS))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String empno) {
        return Jwts.builder()
                .subject(empno)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP_MS))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;

        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();

        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();

        } catch (JwtException e) {
            throw new JwtInvalidException();
        }
    }

    public String getEmpno(String token) {
        return parseClaims(token).getSubject();
    }

    // ⭐ Refresh Token 조회만 유지
    public String resolveRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public long getRemainingExpiration(String token) {
        try {
            long now = System.currentTimeMillis();
            long exp = parseClaims(token).getExpiration().getTime();
            long remain = exp - now;

            // 최소 1초는 보장
            return remain > 1000 ? remain : 1000;

        } catch (Exception e) {
            return 1000;
        }
    }

    public String resolveAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
