package com.cloudrangers.cloudpilot.domain.pipeline;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QJob is a Querydsl query type for Job
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QJob extends EntityPathBase<Job> {

    private static final long serialVersionUID = 1764428712L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QJob job = new QJob("job");

    public final DateTimePath<java.time.Instant> finishedAt = createDateTime("finishedAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath outputLog = createString("outputLog");

    public final QPipeline pipeline;

    public final DateTimePath<java.time.Instant> startedAt = createDateTime("startedAt", java.time.Instant.class);

    public final EnumPath<com.cloudrangers.cloudpilot.enums.JobStatus> status = createEnum("status", com.cloudrangers.cloudpilot.enums.JobStatus.class);

    public final NumberPath<Integer> stepOrder = createNumber("stepOrder", Integer.class);

    public final EnumPath<com.cloudrangers.cloudpilot.enums.JobType> type = createEnum("type", com.cloudrangers.cloudpilot.enums.JobType.class);

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public QJob(String variable) {
        this(Job.class, forVariable(variable), INITS);
    }

    public QJob(Path<? extends Job> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QJob(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QJob(PathMetadata metadata, PathInits inits) {
        this(Job.class, metadata, inits);
    }

    public QJob(Class<? extends Job> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.pipeline = inits.isInitialized("pipeline") ? new QPipeline(forProperty("pipeline"), inits.get("pipeline")) : null;
    }

}

