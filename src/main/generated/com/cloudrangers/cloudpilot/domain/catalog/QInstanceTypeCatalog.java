package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QInstanceTypeCatalog is a Querydsl query type for InstanceTypeCatalog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInstanceTypeCatalog extends EntityPathBase<InstanceTypeCatalog> {

    private static final long serialVersionUID = -1087406082L;

    public static final QInstanceTypeCatalog instanceTypeCatalog = new QInstanceTypeCatalog("instanceTypeCatalog");

    public final BooleanPath burstable = createBoolean("burstable");

    public final StringPath id = createString("id");

    public final NumberPath<Integer> memoryGiB = createNumber("memoryGiB", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final NumberPath<Integer> vcpu = createNumber("vcpu", Integer.class);

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QInstanceTypeCatalog(String variable) {
        super(InstanceTypeCatalog.class, forVariable(variable));
    }

    public QInstanceTypeCatalog(Path<? extends InstanceTypeCatalog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInstanceTypeCatalog(PathMetadata metadata) {
        super(InstanceTypeCatalog.class, metadata);
    }

}

