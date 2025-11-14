package com.cloudrangers.cloudpilot.repository.user;

import com.cloudrangers.cloudpilot.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmpno(Long empno);
    Optional<User> findByEmail(String email);

    // ✅ 로그인 시 user → userRoles → role, team 까지 즉시 로딩
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.team"})
    Optional<User> findWithRolesByEmpno(Long empno);
}
