package com.cloudrangers.cloudpilot.config;

import com.cloudrangers.cloudpilot.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 로그인 / 리프레시
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()

                        // 팀/헤드 정보 (개발 단계에서 오픈)
                        .requestMatchers(
                                "/users/me/team",
                                "/users/me/head",
                                "/users/me/all-team"
                        ).permitAll()

                        // 내 정보는 인증 필요
                        .requestMatchers("/auth/me").authenticated()

                        // 관리자/헤드 권한 필요한 애들
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/packages/approve/**").hasAnyRole("HEAD", "LEADER")

                        // ⭐ OPTIONS Preflight 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ⭐ 리스트 조회는 공개
                        .requestMatchers("/api/packages").permitAll()
                        .requestMatchers("/vms").permitAll()

                        // ⭐🔥 SSE는 인증 제외 → 반드시 추가
                        .requestMatchers("/sse/**").permitAll()

                        // ⭐ 설치 요청은 로그인 필요!
                        .requestMatchers(HttpMethod.POST, "/packages/install").authenticated()

                        .anyRequest().authenticated()
                )

        // JWT 필터
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // ⭐ Authorization 명시적으로 추가 — 매우 중요!!
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));

        config.setAllowCredentials(true);

        // ⭐ Authorization, Set-Cookie 모두 노출
        config.setExposedHeaders(List.of("Set-Cookie", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/sse/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
