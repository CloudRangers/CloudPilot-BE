package com.cloudrangers.cloudpilot.domain.vm;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QVmInstance is a Querydsl query type for VmInstance
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVmInstance extends EntityPathBase<VmInstance> {

    private static final long serialVersionUID = 1924331948L;

    public static final QVmInstance vmInstance = new QVmInstance("vmInstance");

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath lifecycle = createString("lifecycle");

    public final NumberPath<Integer> memoryMb = createNumber("memoryMb", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> ownerUserId = createNumber("ownerUserId", Long.class);

    public final StringPath powerState = createString("powerState");

    public final StringPath providerType = createString("providerType");

    public final NumberPath<Integer> rootDiskGb = createNumber("rootDiskGb", Integer.class);

    public final StringPath tags = createString("tags");

    public final NumberPath<Long> teamId = createNumber("teamId", Long.class);

    public final NumberPath<Integer> vcpu = createNumber("vcpu", Integer.class);

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QVmInstance(String variable) {
        super(VmInstance.class, forVariable(variable));
    }

    public QVmInstance(Path<? extends VmInstance> path) {
        super(path.getType(), path.getMetadata());
    }

    public QVmInstance(PathMetadata metadata) {
        super(VmInstance.class, metadata);
    }

}

