package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTerraformModule is a Querydsl query type for TerraformModule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTerraformModule extends EntityPathBase<TerraformModule> {

    private static final long serialVersionUID = -1016318796L;

    public static final QTerraformModule terraformModule = new QTerraformModule("terraformModule");

    public final StringPath id = createString("id");

    public final StringPath moduleName = createString("moduleName");

    public final StringPath providerType = createString("providerType");

    public final ListPath<String, StringPath> variables = this.<String, StringPath>createList("variables", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath version = createString("version");

    public QTerraformModule(String variable) {
        super(TerraformModule.class, forVariable(variable));
    }

    public QTerraformModule(Path<? extends TerraformModule> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTerraformModule(PathMetadata metadata) {
        super(TerraformModule.class, metadata);
    }

}

