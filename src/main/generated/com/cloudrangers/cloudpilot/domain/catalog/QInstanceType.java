package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QInstanceType is a Querydsl query type for InstanceType
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInstanceType extends EntityPathBase<InstanceType> {

    private static final long serialVersionUID = 1587364763L;

    public static final QInstanceType instanceType = new QInstanceType("instanceType");

    public final BooleanPath burstable = createBoolean("burstable");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> diskGb = createNumber("diskGb", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> memoryGiB = createNumber("memoryGiB", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final NumberPath<Integer> vcpu = createNumber("vcpu", Integer.class);

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QInstanceType(String variable) {
        super(InstanceType.class, forVariable(variable));
    }

    public QInstanceType(Path<? extends InstanceType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInstanceType(PathMetadata metadata) {
        super(InstanceType.class, metadata);
    }

}

