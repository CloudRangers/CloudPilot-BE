package com.cloudrangers.cloudpilot.service.user;

import com.cloudrangers.cloudpilot.domain.user.Role;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.repository.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserRoleRepository userRoleRepository;

    /**
     * 사용자에게 특정 권한이 있는지 확인
     * 예: hasPermission(userId, "vm.create")
     */
    public boolean hasPermission(Long userId, String permissionKey) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        for (UserRole ur : userRoles) {
            Role role = ur.getRole();
            Map<String, Object> perm = role.getPermissions();

            if (perm != null && Boolean.TRUE.equals(perm.get(permissionKey))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 권한이 없으면 예외 발생 — 컨트롤러/서비스에서 직접 호출 가능
     */
    public void checkPermission(Long userId, String permissionKey) {
        if (!hasPermission(userId, permissionKey)) {
            throw new RuntimeException("권한 부족: " + permissionKey);
        }
    }

    /**
     * 현재 사용자 권한 전체 반환 — 마이페이지/관리자 페이지에서 활용 가능
     */
    public Map<String, Object> getAllPermissions(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);

        // 가장 높은 레벨(Role.permissionLevel이 최대인 role) 기준으로 반환 (일종의 merge)
        return userRoles.stream()
                .map(ur -> ur.getRole().getPermissions())
                .filter(p -> p != null)
                .findFirst()
                .orElse(Map.of());
    }
}