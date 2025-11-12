// src/main/java/com/cloudrangers/cloudpilot/config/CatalogLocalSeeder.java
package com.cloudrangers.cloudpilot.config;

import com.cloudrangers.cloudpilot.domain.catalog.InstanceTypeCatalog;
import com.cloudrangers.cloudpilot.domain.catalog.Datastore;
import com.cloudrangers.cloudpilot.domain.catalog.Playbook;
import com.cloudrangers.cloudpilot.domain.catalog.PackageCatalog;
import com.cloudrangers.cloudpilot.repository.catalog.InstanceTypeRepository;
import com.cloudrangers.cloudpilot.repository.catalog.DatastoreRepository;
import com.cloudrangers.cloudpilot.repository.catalog.PlaybookRepository;
import com.cloudrangers.cloudpilot.repository.catalog.PackageCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class CatalogLocalSeeder implements CommandLineRunner {

    private final InstanceTypeRepository itRepo;
    private final DatastoreRepository dsRepo;
    private final PlaybookRepository playbookRepository;              // ✅ 추가
    private final PackageCatalogRepository packageRepository;         // ✅ 추가

    @Override
    public void run(String... args) {
        // 인스턴스 타입 더미
        if (itRepo.count() == 0) {
            itRepo.save(InstanceTypeCatalog.builder()
                    .id("t3.micro")
                    .name("t3.micro")
                    .vcpu(2)
                    .memoryGiB(1)
                    .providerId(1L)
                    .zoneId(10L)
                    .burstable(true)
                    .build());

            itRepo.save(InstanceTypeCatalog.builder()
                    .id("custom-2c4g")
                    .name("2vCPU-4GiB")
                    .vcpu(2)
                    .memoryGiB(4)
                    .providerId(2L)
                    .zoneId(11L)
                    .burstable(false)
                    .build());
        }

        // 데이터스토어 더미
        if (dsRepo.count() == 0) {
            dsRepo.save(Datastore.builder()
                    .id("ds-vsan-01")          // ← code() 아님, id()
                    .name("vsanDatastore01")
                    .type("vsan")
                    .totalGiB(5000)
                    .freeGiB(1200)
                    .zoneId(11L)
                    .providerId(2L)
                    .build());

            dsRepo.save(Datastore.builder()
                    .id("ds-nfs-01")           // ← code() 아님, id()
                    .name("nfsShare01")
                    .type("nfs")
                    .totalGiB(2000)
                    .freeGiB(300)
                    .zoneId(11L)
                    .providerId(2L)
                    .build());
        }

        // 플레이북 더미
        if (playbookRepository.count() == 0) {
            playbookRepository.save(Playbook.builder()
                    .id("pb-java17")
                    .name("install-java17")
                    .tags("java,jdk")
                    .osFamily("ubuntu,centos")
                    .arch("x86_64,arm64")
                    .requiredVars("java_version")
                    .build());

            playbookRepository.save(Playbook.builder()
                    .id("pb-nginx")
                    .name("install-nginx")
                    .tags("nginx,web")
                    .osFamily("ubuntu")
                    .arch("x86_64")
                    .requiredVars("")
                    .build());
        }

        // 패키지 더미
        if (packageRepository.count() == 0) {
            packageRepository.save(PackageCatalog.builder()
                    .name("jdk")
                    .version("17.0.11+9")
                    .osFamily("ubuntu,centos")
                    .arch("x86_64,arm64")
                    .repo("internal-artifact")
                    .build());

            packageRepository.save(PackageCatalog.builder()
                    .name("nginx")
                    .version("1.25.5")
                    .osFamily("ubuntu")
                    .arch("x86_64")
                    .repo("internal-apt")
                    .build());
        }
    }
}
