package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.Datastore;
import org.springframework.data.jpa.domain.Specification;

public final class DatastoreSpecs {
    private DatastoreSpecs(){}

    public static Specification<Datastore> alwaysTrue() {
        return (root, q, cb) -> cb.conjunction();
    }

    public static Specification<Datastore> providerEquals(Long providerId) {
        if (providerId == null) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("providerId"), providerId);
    }

    public static Specification<Datastore> zoneEquals(Long zoneId) {
        if (zoneId == null) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("zoneId"), zoneId);
    }

    public static Specification<Datastore> nameContains(String qStr) {
        if (qStr == null || qStr.isBlank()) return alwaysTrue();
        return (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%" + qStr.toLowerCase() + "%");
    }

    public static Specification<Datastore> typeEquals(String type) {
        if (type == null || type.isBlank()) return alwaysTrue();
        return (root, q, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Datastore> freeGte(Integer minFreeGiB) {
        if (minFreeGiB == null) return alwaysTrue();
        return (root, q, cb) -> cb.ge(root.get("freeGiB"), minFreeGiB);
    }
}
