package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import org.springframework.data.jpa.domain.Specification;

public class OsImageSpecs {
    public static Specification<OsImage> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }
    public static Specification<OsImage> providerEquals(Long providerId) {
        return (root, q, cb) -> providerId == null ? cb.conjunction() : cb.equal(root.get("providerId"), providerId);
    }
    public static Specification<OsImage> zoneEquals(Long zoneId) {
        return (root, q, cb) -> zoneId == null ? cb.conjunction() : cb.equal(root.get("zoneId"), zoneId);
    }
    public static Specification<OsImage> nameOrFamilyContains(String qStr) {
        return (root, q, cb) -> {
            if (qStr == null || qStr.isBlank()) return cb.conjunction();
            String like = "%" + qStr.toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("osFamily")), like));
        };
    }
}
