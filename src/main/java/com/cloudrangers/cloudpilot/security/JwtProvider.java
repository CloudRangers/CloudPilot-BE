package com.cloudrangers.cloudpilot.security;

import com.cloudrangers.cloudpilot.exception.jwt.JwtExpiredException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtInvalidException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtMissingSecretException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    private static final long ACCESS_TOKEN_EXP_MS  = 1000L * 60 * 30;      // 30분
    private static final long REFRESH_TOKEN_EXP_MS = 1000L * 60 * 60 * 24 * 14; // 14일

    private Key getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new JwtMissingSecretException();
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String empno, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(empno)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXP_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String empno) {
        return Jwts.builder()
                .setSubject(empno)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();
        } catch (JwtException e) {
            throw new JwtInvalidException();
        }
    }

    public String getEmpno(String token) {
        return parseClaims(token).getSubject();
    }

    public long getRemainingExpiration(String token) {
        try {
            long now = System.currentTimeMillis();
            long exp = parseClaims(token).getExpiration().getTime();
            return Math.max(exp - now, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public String generateTokenWithClaims(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXP_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
