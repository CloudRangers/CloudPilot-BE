package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.InstanceTypeCatalog;
import org.springframework.data.jpa.domain.Specification;

public final class InstanceTypeSpecs {
    private InstanceTypeSpecs(){}

    public static Specification<InstanceTypeCatalog> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }

    public static Specification<InstanceTypeCatalog> providerEquals(Long providerId) {
        if (providerId == null) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("providerId"), providerId);
    }

    public static Specification<InstanceTypeCatalog> zoneEquals(Long zoneId) {
        if (zoneId == null) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("zoneId"), zoneId);
    }

    public static Specification<InstanceTypeCatalog> nameContains(String qStr) {
        if (qStr == null || qStr.isBlank()) return alwaysTrue();
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + qStr.toLowerCase() + "%");
    }

    public static Specification<InstanceTypeCatalog> vcpuGte(Integer v) {
        if (v == null) return alwaysTrue();
        return (root, q, cb) -> cb.ge(root.get("vcpu"), v);
    }

    public static Specification<InstanceTypeCatalog> vcpuLte(Integer v) {
        if (v == null) return alwaysTrue();
        return (root, q, cb) -> cb.le(root.get("vcpu"), v);
    }

    public static Specification<InstanceTypeCatalog> memGte(Integer g) {
        if (g == null) return alwaysTrue();
        return (root, q, cb) -> cb.ge(root.get("memoryGiB"), g);
    }

    public static Specification<InstanceTypeCatalog> memLte(Integer g) {
        if (g == null) return alwaysTrue();
        return (root, q, cb) -> cb.le(root.get("memoryGiB"), g);
    }

    public static Specification<InstanceTypeCatalog> burstableEq(Boolean b) {
        if (b == null) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("burstable"), b);
    }
}
