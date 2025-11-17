package com.cloudrangers.cloudpilot.repository.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.request.VmSearchCondition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
public class VmInstanceRepositoryImpl implements VmInstanceRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<VmInstance> search(VmSearchCondition c, Pageable pageable, String sort) {
        // ---- 동적 WHERE 구성 ----
        StringBuilder jpql = new StringBuilder("select v from VmInstance v where 1=1");
        StringBuilder countJpql = new StringBuilder("select count(v) from VmInstance v where 1=1");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(c.getProviderType())) {
            jpql.append(" and v.providerType = :providerType");
            countJpql.append(" and v.providerType = :providerType");
            params.put("providerType", c.getProviderType());
        }
        if (c.getZoneId() != null) {
            jpql.append(" and v.zoneId = :zoneId");
            countJpql.append(" and v.zoneId = :zoneId");
            params.put("zoneId", c.getZoneId());
        }
        if (StringUtils.hasText(c.getStatus())) { // lifecycle 매핑
            jpql.append(" and v.lifecycle = :lifecycle");
            countJpql.append(" and v.lifecycle = :lifecycle");
            params.put("lifecycle", c.getStatus());
        }
        if (StringUtils.hasText(c.getPowerState())) {
            jpql.append(" and v.powerState = :powerState");
            countJpql.append(" and v.powerState = :powerState");
            params.put("powerState", c.getPowerState());
        }
        if (StringUtils.hasText(c.getNameContains())) {
            jpql.append(" and v.name like :nameLike");
            countJpql.append(" and v.name like :nameLike");
            params.put("nameLike", "%" + c.getNameContains() + "%");
        }
        if (c.getOwnerUserId() != null) { // createdBy 매핑
            jpql.append(" and v.createdBy = :ownerUserId");
            countJpql.append(" and v.createdBy = :ownerUserId");
            params.put("ownerUserId", c.getOwnerUserId());
        }
        if (c.getTeamId() != null) {
            jpql.append(" and v.teamId = :teamId");
            countJpql.append(" and v.teamId = :teamId");
            params.put("teamId", c.getTeamId());
        }
        Instant from = c.getCreatedFrom();
        Instant to   = c.getCreatedTo();
        if (from != null) {
            jpql.append(" and v.createdAt >= :createdFrom");
            countJpql.append(" and v.createdAt >= :createdFrom");
            params.put("createdFrom", from);
        }
        if (to != null) {
            jpql.append(" and v.createdAt < :createdTo");
            countJpql.append(" and v.createdAt < :createdTo");
            params.put("createdTo", to);
        }

        // NOTE: tagEquals(JSON)은 JPQL로 직접 필터링 어렵기 때문에 여기선 미지원 처리(무시).
        // 추후 MySQL JSON_EXTRACT(nativeQuery) 또는 AttributeConverter/hibernate-types로 확장 가능.

        // ---- 정렬 ----
        String order = resolveOrder(sort);
        jpql.append(order);

        // ---- 쿼리 실행 ----
        TypedQuery<VmInstance> dataQuery = em.createQuery(jpql.toString(), VmInstance.class);
        TypedQuery<Long> countQuery = em.createQuery(countJpql.toString(), Long.class);
        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(dataQuery.getResultList(), pageable, countQuery.getSingleResult());
    }

    /** "createdAt,desc" 같은 문자열을 JPQL order by로 변환 */
    private String resolveOrder(String sort) {
        String fallback = " order by v.createdAt desc";

        if (!StringUtils.hasText(sort)) return fallback;

        // 허용 컬럼 화이트리스트(엔티티 필드명 기준)
        String sortStr = sort.trim();
        String[] parts = sortStr.split(",");
        String field = parts[0].trim();
        String dir = (parts.length > 1 ? parts[1].trim().toLowerCase() : "asc");

        boolean desc = dir.equals("desc");

        switch (field) {
            case "createdAt":
            case "updatedAt":
            case "name":
            case "providerType":
            case "zoneId":
            case "powerState":
            case "lifecycle":
                return " order by v." + field + (desc ? " desc" : " asc");
            default:
                // 미허용 정렬 필드가 들어오면 안전하게 기본값
                return fallback;
        }
    }
}
