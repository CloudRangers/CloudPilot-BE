// src/main/java/com/cloudrangers/cloudpilot/repository/catalog/ZoneSpecs.java
package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.Zone;
import org.springframework.data.jpa.domain.Specification;

public final class ZoneSpecs {

    private ZoneSpecs() {}

    public static Specification<Zone> nameContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            return cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
        };
    }

    public static Specification<Zone> typeEquals(String providerType) {
        return (root, query, cb) -> {
            if (providerType == null || providerType.isBlank()) return null;
            return cb.equal(root.get("providerType"), providerType);
        };
    }

    public static Specification<Zone> providerIdEquals(Long providerId) {
        return (root, query, cb) -> {
            if (providerId == null) return null;
            return cb.equal(root.get("providerId"), providerId);
        };
    }
}
