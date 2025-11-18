package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QProvider is a Querydsl query type for Provider
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProvider extends EntityPathBase<Provider> {

    private static final long serialVersionUID = 266795549L;

    public static final QProvider provider = new QProvider("provider");

    public final StringPath accountId = createString("accountId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath providerType = createString("providerType");

    public QProvider(String variable) {
        super(Provider.class, forVariable(variable));
    }

    public QProvider(Path<? extends Provider> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProvider(PathMetadata metadata) {
        super(Provider.class, metadata);
    }

}

