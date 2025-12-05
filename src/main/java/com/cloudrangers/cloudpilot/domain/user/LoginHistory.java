package com.cloudrangers.cloudpilot.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 유저 PK (User 엔티티 id)
    @Column(nullable = false)
    private Long userId;

    // 역할 코드 (예: ADMIN / HEAD / LEADER / MEMBER)
    @Column(nullable = false, length = 30)
    private String userRole;

    // 로그인 시각
    @Column(nullable = false)
    private LocalDateTime loginAt;
}
