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

    private static final long ACCESS_EXPIRATION_MS = 1000 * 60 * 60; // 1시간

    private Key getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new JwtMissingSecretException();
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** ✅ (1) 기본 Access Token 생성 (empno만 포함) */
    public String generateToken(String empno) {
        return Jwts.builder()
                .setSubject(empno)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ (2) 커스텀 Claims를 포함한 Token 생성 */
    public String generateTokenWithClaims(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** ✅ (3) 오버로드 버전 — empno, role, team 포함 */
    public String generateToken(String empno, String role, String team) {
        return generateTokenWithClaims(empno, Map.of("role", role, "team", team));
    }

    /** ✅ 토큰 유효성 검증 */
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

    /** ✅ 토큰에서 empno 추출 */
    public String getEmpno(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }

    /** ✅ 토큰에서 Claims 전체 추출 */
    public Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtInvalidException();
        }
    }

    /** ✅ 남은 만료 시간 계산 (Redis TTL 용도) */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = getClaims(token);
            long now = System.currentTimeMillis();
            long expiration = claims.getExpiration().getTime();
            return Math.max(expiration - now, 0);
        } catch (JwtException e) {
            return 0;
        }
    }
}
