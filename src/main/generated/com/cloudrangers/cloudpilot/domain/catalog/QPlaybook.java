package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPlaybook is a Querydsl query type for Playbook
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPlaybook extends EntityPathBase<Playbook> {

    private static final long serialVersionUID = -1161494455L;

    public static final QPlaybook playbook = new QPlaybook("playbook");

    public final StringPath arch = createString("arch");

    public final StringPath id = createString("id");

    public final StringPath name = createString("name");

    public final StringPath osFamily = createString("osFamily");

    public final StringPath requiredVars = createString("requiredVars");

    public final StringPath tags = createString("tags");

    public QPlaybook(String variable) {
        super(Playbook.class, forVariable(variable));
    }

    public QPlaybook(Path<? extends Playbook> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPlaybook(PathMetadata metadata) {
        super(Playbook.class, metadata);
    }

}

