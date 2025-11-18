package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDatastore is a Querydsl query type for Datastore
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDatastore extends EntityPathBase<Datastore> {

    private static final long serialVersionUID = -116599605L;

    public static final QDatastore datastore = new QDatastore("datastore");

    public final NumberPath<Integer> freeGiB = createNumber("freeGiB", Integer.class);

    public final StringPath id = createString("id");

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final NumberPath<Integer> totalGiB = createNumber("totalGiB", Integer.class);

    public final StringPath type = createString("type");

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QDatastore(String variable) {
        super(Datastore.class, forVariable(variable));
    }

    public QDatastore(Path<? extends Datastore> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDatastore(PathMetadata metadata) {
        super(Datastore.class, metadata);
    }

}

