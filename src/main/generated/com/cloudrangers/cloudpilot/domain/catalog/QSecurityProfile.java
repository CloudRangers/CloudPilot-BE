package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSecurityProfile is a Querydsl query type for SecurityProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSecurityProfile extends EntityPathBase<SecurityProfile> {

    private static final long serialVersionUID = 1150436509L;

    public static final QSecurityProfile securityProfile = new QSecurityProfile("securityProfile");

    public final StringPath id = createString("id");

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final StringPath rulesCsv = createString("rulesCsv");

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QSecurityProfile(String variable) {
        super(SecurityProfile.class, forVariable(variable));
    }

    public QSecurityProfile(Path<? extends SecurityProfile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSecurityProfile(PathMetadata metadata) {
        super(SecurityProfile.class, metadata);
    }

}

