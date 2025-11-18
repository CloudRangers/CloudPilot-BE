package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPackageCatalog is a Querydsl query type for PackageCatalog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPackageCatalog extends EntityPathBase<PackageCatalog> {

    private static final long serialVersionUID = 1456516127L;

    public static final QPackageCatalog packageCatalog = new QPackageCatalog("packageCatalog");

    public final StringPath arch = createString("arch");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath osFamily = createString("osFamily");

    public final StringPath repo = createString("repo");

    public final StringPath version = createString("version");

    public QPackageCatalog(String variable) {
        super(PackageCatalog.class, forVariable(variable));
    }

    public QPackageCatalog(Path<? extends PackageCatalog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPackageCatalog(PathMetadata metadata) {
        super(PackageCatalog.class, metadata);
    }

}

