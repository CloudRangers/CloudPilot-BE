package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.NetworkSubnet;
import org.springframework.data.jpa.domain.Specification;

public final class NetworkSpecs {
    private NetworkSpecs() {}

    public static Specification<NetworkSubnet> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }

    public static Specification<NetworkSubnet> nameContains(String q) {
        if (q == null || q.isBlank()) return alwaysTrue();
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<NetworkSubnet> providerEquals(Long providerId) {
        if (providerId == null) return alwaysTrue();
        return (root, query, cb) -> cb.equal(root.get("providerId"), providerId);
    }

    public static Specification<NetworkSubnet> zoneEquals(Long zoneId) {
        if (zoneId == null) return alwaysTrue();
        return (root, query, cb) -> cb.equal(root.get("zoneId"), zoneId);
    }
}
