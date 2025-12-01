package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.dto.response.TokenRefreshResponse;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidPasswordException;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidTokenException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtExpiredException;
import com.cloudrangers.cloudpilot.exception.jwt.JwtInvalidException;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 로그인 처리
     * ✔ 토큰 생성은 Controller에서 진행
     * ✔ 여기서는 유저 정보만 반환
     */
    @Override
    public LoginResponse login(@NonNull LoginRequest request) {

        // 1) 사용자 조회
        User user = userRepository.findWithRolesByEmpno(request.getEmpno())
                .orElseThrow(() -> new UserNotFoundException(request.getEmpno()));

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        // 3) 최고 권한(role) + 팀 구하기
        var userRole = getHighestUserRole(user);
        var role = userRole.getRole();
        var team = userRole.getTeam();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : "GLOBAL")
                .build();
    }

    /**
     * RefreshToken → AccessToken 재발급
     */
    @Override
    public TokenRefreshResponse refresh(HttpServletRequest request) {

        String oldRefreshToken = extractRefreshTokenFromCookies(request);
        if (oldRefreshToken == null) {
            throw new InvalidTokenException("refresh_token 쿠키가 없습니다.");
        }

        if (redisTemplate.hasKey("BLACKLIST:" + oldRefreshToken)) {
            throw new InvalidTokenException("만료되었거나 로그아웃된 토큰입니다.");
        }

        if (!jwtProvider.validateToken(oldRefreshToken)) {
            throw new InvalidTokenException("유효하지 않은 리프레시 토큰입니다.");
        }
        
        // 1. 즉시 기존 리프레시 토큰을 블랙리스트에 추가 (Rotation)
        long exp = jwtProvider.getRemainingExpiration(oldRefreshToken);
        redisTemplate.opsForValue().set(
                "BLACKLIST:" + oldRefreshToken,
                "rotated",
                exp,
                TimeUnit.MILLISECONDS
        );

        // 2. 새로운 토큰 생성
        String empno = jwtProvider.getEmpno(oldRefreshToken);
        Map<String, Object> claims = buildClaims(empno);
        String newAccessToken = jwtProvider.generateAccessToken(empno, claims);
        String newRefreshToken = jwtProvider.generateRefreshToken(empno);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃
     * ✔ Access + Refresh 모두 블랙리스트 처리
     */
    @Override
    public void logout(HttpServletRequest request) {

        // Access Token 블랙리스트
        String accessToken = extractAccessTokenFromCookies(request);
        if (accessToken != null) {
            try {
                if (jwtProvider.validateToken(accessToken)) {
                    long exp = jwtProvider.getRemainingExpiration(accessToken);
                    redisTemplate.opsForValue().set(
                            "BLACKLIST:" + accessToken,
                            "logout",
                            exp,
                            TimeUnit.MILLISECONDS
                    );
                }
            } catch (JwtExpiredException e) {
                log.info("로그아웃 처리 중 만료된 Access Token 발견 (블랙리스트 불필요)");
            } catch (JwtInvalidException e) {
                log.warn("로그아웃 처리 중 유효하지 않은 Access Token 발견: {}", e.getMessage());
            }
        }

        // Refresh Token 블랙리스트
        String refreshToken = extractRefreshTokenFromCookies(request);
        if (refreshToken != null) {
            try {
                if (jwtProvider.validateToken(refreshToken)) {
                    long exp = jwtProvider.getRemainingExpiration(refreshToken);
                    redisTemplate.opsForValue().set(
                            "BLACKLIST:" + refreshToken,
                            "logout",
                            exp,
                            TimeUnit.MILLISECONDS
                    );
                }
            } catch (JwtExpiredException e) {
                log.info("로그아웃 처리 중 만료된 Refresh Token 발견 (블랙리스트 불필요)");
            } catch (JwtInvalidException e) {
                log.warn("로그아웃 처리 중 유효하지 않은 Refresh Token 발견: {}", e.getMessage());
            }
        }

        log.info("로그아웃 완료 → Access & Refresh 블랙리스트 처리됨");
    }


    @Override
    public void sendPasswordResetEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        String resetToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                "PWD_RESET_TOKEN:" + resetToken,
                email,
                15,
                TimeUnit.MINUTES
        );

        log.info("비밀번호 재설정 토큰 발급: {}", resetToken);
    }

    @Override
    public void confirmPasswordReset(String token, String newPassword) {

        String email = (String) redisTemplate.opsForValue()
                .get("PWD_RESET_TOKEN:" + token);

        if (email == null) {
            throw new InvalidTokenException("비밀번호 재설정 토큰이 유효하지 않거나 만료되었습니다.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete("PWD_RESET_TOKEN:" + token);

        log.info("비밀번호 재설정 완료: {}", email);
    }

    @Override
    public void changePassword(String currentPassword, String newPassword) {

        Long empno = Long.valueOf(
                SecurityContextHolder.getContext().getAuthentication().getName()
        );

        User user = userRepository.findByEmpno(empno)
                .orElseThrow(() -> new UserNotFoundException(empno));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("비밀번호 변경 완료: {}", empno);
    }

    /**
     * JWT에 넣을 Claims 생성
     */
    @Override
    public Map<String, Object> buildClaims(String empno) {

        User user = userRepository.findWithRolesByEmpno(Long.valueOf(empno))
                .orElseThrow(() -> new UserNotFoundException(empno));

        UserRole userRole = getHighestUserRole(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("empno", user.getEmpno());
        claims.put("username", user.getUsername());
        claims.put("role", userRole.getRole().getCode());
        claims.put("roleName", userRole.getRole().getName());
        claims.put("teamId", userRole.getTeam() != null ? userRole.getTeam().getId() : null);
        claims.put("teamName", userRole.getTeam() != null ? userRole.getTeam().getName() : "GLOBAL");

        return claims;
    }


    /**
     * 가장 권한 높은 UserRole 반환
     */
    private UserRole getHighestUserRole(User user) {
        return user.getUserRoles().stream()
                .max(Comparator.comparingInt(a -> a.getRole().getPermissionLevel()))
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));
    }


    /** ACCESS TOKEN 읽기 */
    private String extractAccessTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** REFRESH TOKEN 읽기 */
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }


    /**
     * 내 정보 조회 (AccessToken 기반)
     */
    @Override
    public LoginResponse getMyInfo(HttpServletRequest request) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof com.cloudrangers.cloudpilot.security.CustomUserDetails)) {
            throw new InvalidTokenException("Access Token expired or invalid.");
        }

        var principal = (com.cloudrangers.cloudpilot.security.CustomUserDetails) authentication.getPrincipal();

        return LoginResponse.builder()
                .username(principal.getUsername())
                .roleCode(principal.getRoleCode())
                .roleName(principal.getRoleName())
                .teamId(principal.getTeamId())
                .teamName(principal.getTeamName())
                .build();
    }

    @Override
    public void updateEmail(Long userId, String newEmail) {
        // 필요 시 구현
    }
}
