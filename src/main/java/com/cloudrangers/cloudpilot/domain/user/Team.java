package com.cloudrangers.cloudpilot.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "team",
        indexes = {
                @Index(name = "idx_team_name", columnList = "name")
        })
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 팀 이름 */
    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    /** 팀 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 생성 시각 (DB DEFAULT CURRENT_TIMESTAMP) */
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}