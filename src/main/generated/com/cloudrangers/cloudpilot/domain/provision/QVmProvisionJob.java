package com.cloudrangers.cloudpilot.domain.provision;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QVmProvisionJob is a Querydsl query type for VmProvisionJob
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVmProvisionJob extends EntityPathBase<VmProvisionJob> {

    private static final long serialVersionUID = -2024900561L;

    public static final QVmProvisionJob vmProvisionJob = new QVmProvisionJob("vmProvisionJob");

    public final NumberPath<Long> catalogId = createNumber("catalogId", Long.class);

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final NumberPath<Long> createdBy = createNumber("createdBy", Long.class);

    public final StringPath errorMessage = createString("errorMessage");

    public final DateTimePath<java.time.Instant> finishedAt = createDateTime("finishedAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath jobUuid = createString("jobUuid");

    public final NumberPath<Integer> maxRetries = createNumber("maxRetries", Integer.class);

    public final StringPath purpose = createString("purpose");

    public final NumberPath<Integer> retryCount = createNumber("retryCount", Integer.class);

    public final StringPath retryGuide = createString("retryGuide");

    public final DateTimePath<java.time.Instant> startedAt = createDateTime("startedAt", java.time.Instant.class);

    public final EnumPath<com.cloudrangers.cloudpilot.enums.VmProvisionStatus> status = createEnum("status", com.cloudrangers.cloudpilot.enums.VmProvisionStatus.class);

    public final NumberPath<Long> teamId = createNumber("teamId", Long.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public final NumberPath<Long> updatedBy = createNumber("updatedBy", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Short> zoneId = createNumber("zoneId", Short.class);

    public QVmProvisionJob(String variable) {
        super(VmProvisionJob.class, forVariable(variable));
    }

    public QVmProvisionJob(Path<? extends VmProvisionJob> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVmProvisionJob(PathMetadata metadata) {
        super(VmProvisionJob.class, metadata);
    }

}

