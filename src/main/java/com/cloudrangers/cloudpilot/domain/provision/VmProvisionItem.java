package com.cloudrangers.cloudpilot.domain.provision;

import com.cloudrangers.cloudpilot.enums.CloneType;
import com.cloudrangers.cloudpilot.enums.DiskProvisioning;
import com.cloudrangers.cloudpilot.enums.IpAllocationMode;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vm_provision_item",
        indexes = {
                @Index(name = "idx_provision_item_job", columnList = "provision_job_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class VmProvisionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관관계: N:1 (Job:Item)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provision_job_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vm_provision_item_job"))
    private VmProvisionJob provisionJob;

    // 외래키는 우선 ID로만
    @Column(name = "os_image_id", nullable = false)
    private Long osImageId;

    @Column(name = "spec_preset_id", nullable = false)
    private Long specPresetId;

    @Column(name = "count", nullable = false)
    private Integer count = 1;

    @Column(name = "name_prefix", length = 100)
    private String namePrefix;

    @Column(name = "folder", length = 200)
    private String folder;

    @Column(name = "resource_pool", length = 200)
    private String resourcePool;

    @Column(name = "datastore", length = 200)
    private String datastore;

    @Column(name = "network", length = 200)
    private String network;

    @Enumerated(EnumType.STRING)
    @Column(name = "ip_allocation_mode", nullable = false, length = 20)
    private IpAllocationMode ipAllocationMode = IpAllocationMode.DHCP;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "static_ip_config", columnDefinition = "json")
    private JsonNode staticIpConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dns_servers", columnDefinition = "json")
    private JsonNode dnsServers;

    @Column(name = "dns_suffix", length = 100)
    private String dnsSuffix;

    @Column(name = "vlan_id")
    private Integer vlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "disk_provisioning", nullable = false, length = 20)
    private DiskProvisioning diskProvisioning = DiskProvisioning.thin;

    @Enumerated(EnumType.STRING)
    @Column(name = "clone_type", nullable = false, length = 20)
    private CloneType cloneType = CloneType.full;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @Column(name = "hostname_pattern", length = 100)
    private String hostnamePattern;

    @Column(name = "annotation", columnDefinition = "TEXT")
    private String annotation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private JsonNode tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spec_override", columnDefinition = "json")
    private JsonNode specOverride;
}
