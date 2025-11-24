package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidPasswordException;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidTokenException;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.servlet.http.Cookie;

import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginResponse login(@NonNull LoginRequest request) {

        User user = userRepository.findWithRolesByEmpno(request.getEmpno())
                .orElseThrow(() -> new UserNotFoundException(request.getEmpno()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        var userRole = getHighestUserRole(user);
        var role = userRole.getRole();
        var team = userRole.getTeam();

        Map<String, Object> claims = buildClaims(user.getEmpno().toString());
        String accessToken = jwtProvider.generateAccessToken(
                user.getEmpno().toString(),
                claims
        );
        String refreshToken = jwtProvider.generateRefreshToken(
                user.getEmpno().toString()
        );

        return LoginResponse.builder()
                .username(user.getUsername())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .teamId(team != null ? team.getId() : null)
                .teamName(team != null ? team.getName() : "GLOBAL")
                .build();
    }

    @Override
    public String refresh(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new InvalidTokenException("refresh_token 쿠키가 없습니다.");
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refresh_token".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
            }
        }

        if (refreshToken == null) {
            throw new InvalidTokenException("refresh_token 쿠키가 없습니다.");
        }

        if (redisTemplate.hasKey("BLACKLIST:" + refreshToken)) {
            throw new InvalidTokenException("만료되었거나 로그아웃된 토큰입니다.");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("유효하지 않은 리프레시 토큰입니다.");
        }

        String empno = jwtProvider.getEmpno(refreshToken);
        Map<String, Object> claims = buildClaims(empno);

        return jwtProvider.generateAccessToken(empno, claims);
    }

    @Override
    public void logout(HttpServletRequest request) {

        String token = extractTokenFromCookies(request);

        if (token == null) {
            throw new InvalidTokenException("로그아웃할 access_token 쿠키가 없습니다.");
        }

        if (!jwtProvider.validateToken(token)) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }

        long expiration = jwtProvider.getRemainingExpiration(token);

        redisTemplate.opsForValue().set(
                "BLACKLIST:" + token,
                "logout",
                expiration,
                TimeUnit.MILLISECONDS
        );

        log.info("로그아웃 완료: {}", token);
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

        String email = (String) redisTemplate.opsForValue().get("PWD_RESET_TOKEN:" + token);

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


    private UserRole getHighestUserRole(User user) {
        return user.getUserRoles().stream()
                .max(Comparator.comparingInt(a -> a.getRole().getPermissionLevel()))
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("access_token")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public void updateEmail(Long userId, String newEmail) {
        // TODO
    }

    @Override
    public LoginResponse getMyInfo(HttpServletRequest request) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidTokenException("인증 정보가 없습니다.");
        }

        // 🔥 JwtAuthenticationFilter에서 저장한 CustomUserDetails 읽기
        Object principalObj = authentication.getPrincipal();

        if (!(principalObj instanceof com.cloudrangers.cloudpilot.security.CustomUserDetails principal)) {
            throw new InvalidTokenException("유효하지 않은 인증 정보입니다.");
        }

        return LoginResponse.builder()
                .username(principal.getUsername())
                .roleCode(principal.getRoleCode())
                .roleName(principal.getRoleName())
                .teamId(principal.getTeamId())
                .teamName(principal.getTeamName())
                .build();
    }


}