package com.cloudrangers.cloudpilot.domain.provision;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QVmProvisionItem is a Querydsl query type for VmProvisionItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QVmProvisionItem extends EntityPathBase<VmProvisionItem> {

    private static final long serialVersionUID = 1652567265L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QVmProvisionItem vmProvisionItem = new QVmProvisionItem("vmProvisionItem");

    public final StringPath annotation = createString("annotation");

    public final EnumPath<com.cloudrangers.cloudpilot.enums.CloneType> cloneType = createEnum("cloneType", com.cloudrangers.cloudpilot.enums.CloneType.class);

    public final NumberPath<Integer> count = createNumber("count", Integer.class);

    public final StringPath datastore = createString("datastore");

    public final EnumPath<com.cloudrangers.cloudpilot.enums.DiskProvisioning> diskProvisioning = createEnum("diskProvisioning", com.cloudrangers.cloudpilot.enums.DiskProvisioning.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> dnsServers = createSimple("dnsServers", com.fasterxml.jackson.databind.JsonNode.class);

    public final StringPath dnsSuffix = createString("dnsSuffix");

    public final StringPath folder = createString("folder");

    public final StringPath guestId = createString("guestId");

    public final StringPath hostnamePattern = createString("hostnamePattern");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.cloudrangers.cloudpilot.enums.IpAllocationMode> ipAllocationMode = createEnum("ipAllocationMode", com.cloudrangers.cloudpilot.enums.IpAllocationMode.class);

    public final StringPath namePrefix = createString("namePrefix");

    public final StringPath network = createString("network");

    public final NumberPath<Long> osImageId = createNumber("osImageId", Long.class);

    public final QVmProvisionJob provisionJob;

    public final StringPath resourcePool = createString("resourcePool");

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> specOverride = createSimple("specOverride", com.fasterxml.jackson.databind.JsonNode.class);

    public final NumberPath<Long> specPresetId = createNumber("specPresetId", Long.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> staticIpConfig = createSimple("staticIpConfig", com.fasterxml.jackson.databind.JsonNode.class);

    public final SimplePath<com.fasterxml.jackson.databind.JsonNode> tags = createSimple("tags", com.fasterxml.jackson.databind.JsonNode.class);

    public final NumberPath<Integer> vlanId = createNumber("vlanId", Integer.class);

    public QVmProvisionItem(String variable) {
        this(VmProvisionItem.class, forVariable(variable), INITS);
    }

    public QVmProvisionItem(Path<? extends VmProvisionItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QVmProvisionItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QVmProvisionItem(PathMetadata metadata, PathInits inits) {
        this(VmProvisionItem.class, metadata, inits);
    }

    public QVmProvisionItem(Class<? extends VmProvisionItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.provisionJob = inits.isInitialized("provisionJob") ? new QVmProvisionJob(forProperty("provisionJob")) : null;
    }

}

