package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.dto.request.LoginRequest;
import com.cloudrangers.cloudpilot.dto.response.LoginResponse;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidPasswordException;
import com.cloudrangers.cloudpilot.exception.badrequest.InvalidTokenException;
import com.cloudrangers.cloudpilot.exception.notfound.UserNotFoundException;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import com.cloudrangers.cloudpilot.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.data.redis.core.RedisTemplate;

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
        // ✅ 1. 사용자 + 역할 + 팀 정보 조회
        User user = userRepository.findWithRolesByEmpno(request.getEmpno())
                .orElseThrow(() -> new UserNotFoundException(request.getEmpno()));

        // ✅ 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        // ✅ 3. 역할 및 팀 추출 (단일 역할 기준)
        var userRole = user.getUserRoles().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("역할 정보가 없습니다."));
        String roleCode = userRole.getRole().getCode();
        String teamName = userRole.getTeam() != null ? userRole.getTeam().getName() : "GLOBAL";
        String username = user.getUsername();


        // ✅ 4. JWT 생성 (empno + role + team)
        String accessToken = jwtProvider.generateTokenWithClaims(
                String.valueOf(user.getEmpno()),
                Map.of(
                        "role", roleCode,
                        "team", teamName
                )
        );

        // ✅ 5. 로그인 응답 반환
        return new LoginResponse(accessToken, roleCode, teamName, username);
    }

    /** ✅ Redis 기반 로그아웃 */
    @Override
    public void logout(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new InvalidTokenException("Authorization 헤더가 유효하지 않습니다.");
        }

        String pureToken = token.substring(7);

        // JWT 검증
        if (!jwtProvider.validateToken(pureToken)) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다.");
        }

        // 토큰 만료까지 남은 시간 계산
        long expiration = jwtProvider.getRemainingExpiration(pureToken);

        // ✅ Redis에 블랙리스트 등록 (key = BLACKLIST:<token>)
        redisTemplate.opsForValue()
                .set("BLACKLIST:" + pureToken, "logout", expiration, TimeUnit.MILLISECONDS);
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
}