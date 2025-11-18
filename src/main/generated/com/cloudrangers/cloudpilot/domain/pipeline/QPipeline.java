package com.cloudrangers.cloudpilot.domain.pipeline;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPipeline is a Querydsl query type for Pipeline
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPipeline extends EntityPathBase<Pipeline> {

    private static final long serialVersionUID = -851872361L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPipeline pipeline = new QPipeline("pipeline");

    public final DateTimePath<java.time.Instant> finishedAt = createDateTime("finishedAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.cloudrangers.cloudpilot.domain.provision.QVmProvisionJob provisionJob;

    public final DateTimePath<java.time.Instant> startedAt = createDateTime("startedAt", java.time.Instant.class);

    public final EnumPath<com.cloudrangers.cloudpilot.enums.PipelineStatus> status = createEnum("status", com.cloudrangers.cloudpilot.enums.PipelineStatus.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public QPipeline(String variable) {
        this(Pipeline.class, forVariable(variable), INITS);
    }

    public QPipeline(Path<? extends Pipeline> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPipeline(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPipeline(PathMetadata metadata, PathInits inits) {
        this(Pipeline.class, metadata, inits);
    }

    public QPipeline(Class<? extends Pipeline> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.provisionJob = inits.isInitialized("provisionJob") ? new com.cloudrangers.cloudpilot.domain.provision.QVmProvisionJob(forProperty("provisionJob")) : null;
    }

}

