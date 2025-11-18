package com.cloudrangers.cloudpilot.security;

import com.cloudrangers.cloudpilot.domain.user.User;
import com.cloudrangers.cloudpilot.domain.user.UserRole;
import com.cloudrangers.cloudpilot.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChecker {

    private final UserRepository userRepository;

    private UserRole highestRole(Long userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return user.getUserRoles().stream()
                .max(Comparator.comparingInt(
                        ur -> Optional.ofNullable(ur.getRole().getPermissionLevel()).orElse(0)
                ))
                .orElseThrow(() -> new RuntimeException("No roles for userId=" + userId));
    }

    private Map<String, Object> rootPerms(UserRole ur) {
        Map<String, Object> map = ur.getRole().getPermissions();
        return map == null ? Map.of() : map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> subPerms(Map<String, Object> root, String key) {
        Object raw = root.get(key);
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new HashMap<>();
            rawMap.forEach((k, v) -> {
                if (k != null) result.put(String.valueOf(k), v);
            });
            return result;
        }
        return Map.of();
    }

    private boolean flag(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s)
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        return false;
    }

    private String normalizeScope(Object scopeObj) {
        if (scopeObj == null) return "self_only";
        String s = String.valueOf(scopeObj).trim().toLowerCase();
        return switch (s) {
            case "multi_team", "multi-team" -> "multi_team";
            case "own_team", "own-team" -> "own_team";
            case "self_only", "self-only", "self" -> "self_only";
            case "system_all" -> "system_all";
            default -> "self_only";
        };
    }

    public boolean canViewVM(Long userId, Long vmTeamId, Long vmOwnerId) {
        UserRole ur = highestRole(userId);
        String roleCode = Optional.ofNullable(ur.getRole().getCode()).orElse("UNKNOWN");
        Long userTeamId = ur.getTeam() != null ? ur.getTeam().getId() : null;

        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> vm = subPerms(root, "vm");
        String scope = normalizeScope(root.get("scope"));

        boolean viewAll = flag(vm, "view_all");
        boolean viewTeam = flag(vm, "view_team");
        boolean viewSelf = flag(vm, "view_self");

        if ("ADMIN".equalsIgnoreCase(roleCode) && "system_all".equals(scope)) return true;
        if (viewAll) return true;
        if (viewTeam && userTeamId != null && userTeamId.equals(vmTeamId)) return true;
        if (viewSelf && userId != null && userId.equals(vmOwnerId)) return true;

        return switch (scope) {
            case "system_all" -> true;
            case "multi_team" -> true;
            case "own_team" -> userTeamId != null && userTeamId.equals(vmTeamId);
            case "self_only" -> userId != null && userId.equals(vmOwnerId);
            default -> false;
        };
    }

    public boolean canApplyPackage(Long userId) {
        UserRole ur = highestRole(userId);
        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> pkg = subPerms(root, "package");
        return flag(pkg, "apply");
    }

    public boolean canApproveL1(Long userId) {
        UserRole ur = highestRole(userId);
        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> pkg = subPerms(root, "package");
        return flag(pkg, "approve_l1");
    }

    public boolean canApproveFinal(Long userId) {
        UserRole ur = highestRole(userId);
        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> pkg = subPerms(root, "package");
        return flag(pkg, "approve_final");
    }

    public boolean canInstallPackage(Long userId, Long vmTeamId, Long vmOwnerId) {
        UserRole ur = highestRole(userId);
        String roleCode = Optional.ofNullable(ur.getRole().getCode()).orElse("UNKNOWN");
        Long userTeamId = ur.getTeam() != null ? ur.getTeam().getId() : null;

        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> pkg = subPerms(root, "package");
        String scope = normalizeScope(root.get("scope"));

        boolean canExecute = flag(pkg, "execute");
        if (!canExecute) return false;
        if ("ADMIN".equalsIgnoreCase(roleCode) && "system_all".equals(scope)) return true;

        return switch (scope) {
            case "system_all" -> true;
            case "multi_team" -> true;
            case "own_team" -> userTeamId != null && userTeamId.equals(vmTeamId);
            case "self_only" -> userId != null && userId.equals(vmOwnerId);
            default -> false;
        };
    }

    public boolean canCreateVM(Long userId, Long targetTeamId) {
        UserRole ur = highestRole(userId);
        String roleCode = ur.getRole().getCode();
        Long userTeamId = ur.getTeam() != null ? ur.getTeam().getId() : null;

        Map<String, Object> root = rootPerms(ur);
        Map<String, Object> vm = subPerms(root, "vm");
        String scope = normalizeScope(root.get("scope"));

        boolean create = flag(vm, "create");
        if (!create) return false;
        if ("ADMIN".equalsIgnoreCase(roleCode) && "system_all".equals(scope)) return true;

        return switch (scope) {
            case "system_all" -> true;
            case "multi_team" -> true;
            case "own_team" -> userTeamId != null && userTeamId.equals(targetTeamId);
            case "self_only" -> false;
            default -> false;
        };
    }
}
