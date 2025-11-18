package com.cloudrangers.cloudpilot.domain.catalog;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QNetworkSubnet is a Querydsl query type for NetworkSubnet
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNetworkSubnet extends EntityPathBase<NetworkSubnet> {

    private static final long serialVersionUID = -1885309313L;

    public static final QNetworkSubnet networkSubnet = new QNetworkSubnet("networkSubnet");

    public final StringPath cidr = createString("cidr");

    public final StringPath id = createString("id");

    public final StringPath name = createString("name");

    public final NumberPath<Long> providerId = createNumber("providerId", Long.class);

    public final StringPath purposeCsv = createString("purposeCsv");

    public final NumberPath<Long> zoneId = createNumber("zoneId", Long.class);

    public QNetworkSubnet(String variable) {
        super(NetworkSubnet.class, forVariable(variable));
    }

    public QNetworkSubnet(Path<? extends NetworkSubnet> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNetworkSubnet(PathMetadata metadata) {
        super(NetworkSubnet.class, metadata);
    }

}

