package com.cloudrangers.cloudpilot.repository.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmpno(Long empno);
    Optional<User> findByEmail(String email);

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
}
