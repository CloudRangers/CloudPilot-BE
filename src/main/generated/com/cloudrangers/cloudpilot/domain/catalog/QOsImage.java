package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QOsImage is a Querydsl query type for OsImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOsImage extends EntityPathBase<OsImage> {

    private static final long serialVersionUID = -192901077L;

    public static final QOsImage osImage = new QOsImage("osImage");

    public final StringPath arch = createString("arch");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageId = createString("imageId");

    public final StringPath name = createString("name");

    public final StringPath osFamily = createString("osFamily");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final StringPath providerType = createString("providerType");

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QOsImage(String variable) {
        super(OsImage.class, forVariable(variable));
    }

    public QOsImage(Path<? extends OsImage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOsImage(PathMetadata metadata) {
        super(OsImage.class, metadata);
    }

}

