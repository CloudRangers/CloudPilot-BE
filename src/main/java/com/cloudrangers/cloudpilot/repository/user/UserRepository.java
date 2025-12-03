// src/main/java/com/cloudrangers/cloudpilot/repository/user/UserRepository.java
package com.cloudrangers.cloudpilot.repository.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmpno(Long empno);
    Optional<User> findByEmail(String email);

    // ⭐ 여기만 이렇게 수정
    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role",
            "userRoles.team"
    })
    Optional<User> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role",
            "userRoles.team"
    })
    Optional<User> findWithRolesByEmpno(Long empno);

    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role",
            "userRoles.team"
    })
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role",
            "userRoles.team"
    })
    Optional<User> findWithRolesByEmail(String email);

    @EntityGraph(attributePaths = {
            "userRoles",
            "userRoles.role",
            "userRoles.team"
    })
    List<User> findByUserRoles_Team_Id(Long teamId);
}
