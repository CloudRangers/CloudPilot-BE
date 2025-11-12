package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.SecurityProfile;
import org.springframework.data.jpa.domain.Specification;

public final class SecurityProfileSpecs {
    private SecurityProfileSpecs() {}

    public static Specification<SecurityProfile> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }

    public static Specification<SecurityProfile> nameContains(String q) {
        if (q == null || q.isBlank()) return alwaysTrue();
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<SecurityProfile> providerEquals(Long providerId) {
        if (providerId == null) return alwaysTrue();
        return (root, query, cb) -> cb.equal(root.get("providerId"), providerId);
    }

    public static Specification<SecurityProfile> zoneEquals(Long zoneId) {
        if (zoneId == null) return alwaysTrue();
        return (root, query, cb) -> cb.equal(root.get("zoneId"), zoneId);
    }
}
