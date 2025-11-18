package com.cloudrangers.cloudpilot.domain.pipeline;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTfRun is a Querydsl query type for TfRun
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTfRun extends EntityPathBase<TfRun> {

    private static final long serialVersionUID = -887134236L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTfRun tfRun = new QTfRun("tfRun");

    public final EnumPath<com.cloudrangers.cloudpilot.enums.TfRunAction> action = createEnum("action", com.cloudrangers.cloudpilot.enums.TfRunAction.class);

    public final DateTimePath<java.time.Instant> finishedAt = createDateTime("finishedAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> moduleVersionId = createNumber("moduleVersionId", Long.class);

    public final QPipeline pipeline;

    public final StringPath planJsonUri = createString("planJsonUri");

    public final DateTimePath<java.time.Instant> startedAt = createDateTime("startedAt", java.time.Instant.class);

    public final StringPath stateBackend = createString("stateBackend");

    public final StringPath stateUri = createString("stateUri");

    public final EnumPath<com.cloudrangers.cloudpilot.enums.TfRunStatus> status = createEnum("status", com.cloudrangers.cloudpilot.enums.TfRunStatus.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public final NumberPath<Long> varsetId = createNumber("varsetId", Long.class);

    public final StringPath workspace = createString("workspace");

    public QTfRun(String variable) {
        this(TfRun.class, forVariable(variable), INITS);
    }

    public QTfRun(Path<? extends TfRun> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTfRun(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTfRun(PathMetadata metadata, PathInits inits) {
        this(TfRun.class, metadata, inits);
    }

    public QTfRun(Class<? extends TfRun> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.pipeline = inits.isInitialized("pipeline") ? new QPipeline(forProperty("pipeline"), inits.get("pipeline")) : null;
    }

}

