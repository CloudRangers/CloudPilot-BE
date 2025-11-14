package com.cloudrangers.cloudpilot.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 예: HEAD / LEADER / MEMBER */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** 역할 이름: 부장 / 팀장 / 팀원 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 권한 우선순위 (숫자 높을수록 상위) */
    @Column(name = "permission_level", nullable = false)
    private Integer permissionLevel;

    /** JSON 필드: {"vm": {...}, "package": {...}} */
    @Column(name = "permissions", columnDefinition = "json")
    @Convert(converter = com.cloudrangers.cloudpilot.domain.user.converter.JsonToMapConverter.class)
    private Map<String, Object> permissions;

    /** UserRole과의 양방향 관계 (선택적) */
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private java.util.List<UserRole> userRoles = new java.util.ArrayList<>();
}
