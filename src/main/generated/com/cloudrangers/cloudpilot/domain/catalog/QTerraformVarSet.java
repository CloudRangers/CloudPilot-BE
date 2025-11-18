package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTerraformVarSet is a Querydsl query type for TerraformVarSet
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTerraformVarSet extends EntityPathBase<TerraformVarSet> {

    private static final long serialVersionUID = -771201533L;

    public static final QTerraformVarSet terraformVarSet = new QTerraformVarSet("terraformVarSet");

    public final StringPath id = createString("id");

    public final StringPath moduleId = createString("moduleId");

    public final StringPath name = createString("name");

    public final EnumPath<VarSetScope> scope = createEnum("scope", VarSetScope.class);

    public final MapPath<String, String, StringPath> variables = this.<String, String, StringPath>createMap("variables", String.class, String.class, StringPath.class);

    public QTerraformVarSet(String variable) {
        super(TerraformVarSet.class, forVariable(variable));
    }

    public QTerraformVarSet(Path<? extends TerraformVarSet> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTerraformVarSet(PathMetadata metadata) {
        super(TerraformVarSet.class, metadata);
    }

}

