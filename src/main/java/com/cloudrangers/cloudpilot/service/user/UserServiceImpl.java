package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidPasswordException;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidTokenException;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import jakarta.servlet.http.Cookie;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

        var userRole = user.getUserRoles().stream()
                .max((a, b) -> a.getRole().getPermissionLevel() - b.getRole().getPermissionLevel())
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));

        var role = userRole.getRole();
        var team = userRole.getTeam();

        String roleCode = role.getCode();
        String roleName = role.getName();
        String scope    = (String) role.getPermissions().get("scope");

        Long teamId = (team != null) ? team.getId() : null;
        String teamName = (team != null) ? team.getName() : "GLOBAL";

        String username = user.getUsername();

        // ❗ claims 생성 (null 제거)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", roleCode);
        claims.put("teamId", teamId);
        claims.put("team", teamName);
        if (scope != null) claims.put("scope", scope);

        // access token 생성
        String accessToken = jwtProvider.generateAccessToken(
                String.valueOf(user.getEmpno()),
                claims
        );

        String refreshToken = jwtProvider.generateRefreshToken(
                String.valueOf(user.getEmpno())
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(username)
                .roleCode(roleCode)
                .roleName(roleName)
                .teamId(teamId)
                .teamName(teamName)
                .build();

    }

    /** ✅ Redis 기반 로그아웃 */
    @Override
    public void logout( HttpServletRequest request) {

        // 1) 쿠키에서 access_token 읽기
        String token = extractTokenFromCookies(request);

        if (token == null) {
            throw new InvalidTokenException("로그아웃할 access_token 쿠키가 없습니다.");
        }

        // 2) JWT 검증
        if (!jwtProvider.validateToken(token)) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }

        // 3) 남은 만료시간 계산
        long expiration = jwtProvider.getRemainingExpiration(token);

        // 4) 블랙리스트 등록
        redisTemplate.opsForValue()
                .set("BLACKLIST:" + token, "logout", expiration, TimeUnit.MILLISECONDS);

        log.info("🚫 로그아웃: 블랙리스트 등록완료 token={}", token);
    }

    // Helper: 쿠키에서 access_token 추출
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
    public void sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // 임시 토큰 생성 (UUID)
        String resetToken = UUID.randomUUID().toString();

        // Redis에 저장 (15분 유효)
        redisTemplate.opsForValue()
                .set("PWD_RESET:" + email, resetToken, 15, TimeUnit.MINUTES);

        // 이메일 전송 대신 로그로 확인 (SMTP 나중에 교체 가능)
        log.info("📩 비밀번호 재설정 토큰 발급: email={}, token={}", email, resetToken);
    }


    @Override
    public void confirmPasswordReset(String token, String newPassword) {
        // Redis에서 해당 토큰 검색
        Optional<String> emailOpt = redisTemplate.keys("PWD_RESET:*").stream()
                .filter(key -> token.equals(redisTemplate.opsForValue().get(key)))
                .map(key -> key.replace("PWD_RESET:", ""))
                .findFirst();

        if (emailOpt.isEmpty()) {
            throw new InvalidTokenException("비밀번호 재설정 토큰이 유효하지 않거나 만료되었습니다.");
        }

        String email = emailOpt.get();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // 새 비밀번호 저장
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 토큰 사용 후 삭제
        redisTemplate.delete("PWD_RESET:" + email);

        log.info("✅ 비밀번호 재설정 완료: {}", email);
    }

    @Override
    public void changePassword(String currentPassword, String newPassword) {
        // TODO: SecurityContextHolder에서 현재 사용자 정보(empno) 추출 후 로직 완성
        // 임시 예시 (테스트용)
        Long empno = 1001L;

        User user = userRepository.findByEmpno(empno)
                .orElseThrow(() -> new UserNotFoundException(empno));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("🔑 비밀번호 변경 완료: {}", empno);
    }

    @Override
    public void updateEmail(Long userId, String newEmail) {
        // TODO: 이메일 변경 로직
    }

    @Override
    public Map<String, Object> buildClaims(String empno) {

        User user = userRepository.findWithRolesByEmpno(Long.valueOf(empno))
                .orElseThrow(() -> new UserNotFoundException(empno));

        var userRole = user.getUserRoles().stream()
                .max((a, b) -> a.getRole().getPermissionLevel() - b.getRole().getPermissionLevel())
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));

        var role = userRole.getRole();
        var team = userRole.getTeam();

        Map<String, Object> claims = new HashMap<>();

        claims.put("role", role.getCode());
        claims.put("teamId", team != null ? team.getId() : null);
        claims.put("team", team != null ? team.getName() : "GLOBAL");

        return claims;
    }
}