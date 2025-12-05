package com.cloudrangers.cloudpilot.repository.user;

import com.cloudrangers.cloudpilot.domain.user.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    long countByUserRoleInAndLoginAtBetween(
            List<String> userRoles,
            LocalDateTime start,
            LocalDateTime end
    );
}
