package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProviderLocation is a Querydsl query type for ProviderLocation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProviderLocation extends EntityPathBase<ProviderLocation> {

    private static final long serialVersionUID = -550006542L;

    public static final QProviderLocation providerLocation = new QProviderLocation("providerLocation");

    public final DateTimePath<java.time.Instant> createdAt = createDateTime("createdAt", java.time.Instant.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final StringPath providerType = createString("providerType");

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public QProviderLocation(String variable) {
        super(ProviderLocation.class, forVariable(variable));
    }

    public QProviderLocation(Path<? extends ProviderLocation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProviderLocation(PathMetadata metadata) {
        super(ProviderLocation.class, metadata);
    }

}

