package com.cloudrangers.cloudpilot.config;

import com.cloudrangers.cloudpilot.domain.catalog.Provider;
import com.cloudrangers.cloudpilot.domain.catalog.Zone;
import com.cloudrangers.cloudpilot.domain.catalog.OsImage;
import com.cloudrangers.cloudpilot.repository.catalog.ProviderRepository;
import com.cloudrangers.cloudpilot.repository.catalog.ZoneRepository;
import com.cloudrangers.cloudpilot.repository.catalog.OsImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProviderRepository providerRepository;
    private final ZoneRepository zoneRepository;
    private final OsImageRepository osImageRepository;

    @Override
    public void run(String... args) {
        // 이미 있으면 Skip
        if (providerRepository.count() > 0) return;

        // Provider Seed
        var aws = providerRepository.save(Provider.builder()
                .name("AWS Default Account")
                .providerType("AWS")
                .accountId("123456789012")
                .build());

        var vsphere = providerRepository.save(Provider.builder()
                .name("On-prem DC Cluster")
                .providerType("VSPHERE")
                .accountId("vsphere-prod")
                .build());

        // Zone Seed
        var seoulA = zoneRepository.save(Zone.builder()
                .name("ap-northeast-2a")
                .providerId(aws.getId())
                .providerType("AWS")
                .build());

        var localCluster = zoneRepository.save(Zone.builder()
                .name("DC1/ClusterA")
                .providerId(vsphere.getId())
                .providerType("VSPHERE")
                .build());

        // OS Image Seed
        osImageRepository.save(OsImage.builder()
                .name("Ubuntu 22.04 LTS")
                .osFamily("LINUX")
                .arch("x86_64")
                .providerId(aws.getId())
                .zoneId(seoulA.getId())
                .build());

        osImageRepository.save(OsImage.builder()
                .name("Windows Server 2019")
                .osFamily("WINDOWS")
                .arch("x86_64")
                .providerId(vsphere.getId())
                .zoneId(localCluster.getId())
                .build());
    }
}
