// src/main/java/com/cloudrangers/cloudpilot/repository/catalog/ProviderSpecs.java
package com.cloudrangers.cloudpilot.repository.catalog;

import com.cloudrangers.cloudpilot.domain.catalog.Provider;
import org.springframework.data.jpa.domain.Specification;

public final class ProviderSpecs {
    private ProviderSpecs() {}

    public static Specification<Provider> nameContains(String q) {
        return (root, cq, cb) ->
                (q == null || q.isBlank()) ? null
                        : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Provider> typeEquals(String type) {
        return (root, cq, cb) ->
                (type == null || type.isBlank()) ? null
                        : cb.equal(root.get("providerType"), type);
    }
}
