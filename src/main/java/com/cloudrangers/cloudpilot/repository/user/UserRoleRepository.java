package com.cloudrangers.cloudpilot.repository.user;

import com.cloudrangers.cloudpilot.domain.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

}
