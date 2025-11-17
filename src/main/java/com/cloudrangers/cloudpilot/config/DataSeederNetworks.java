package com.cloudrangers.cloudpilot.config;

import com.cloudrangers.cloudpilot.domain.catalog.NetworkSubnet;
import com.cloudrangers.cloudpilot.domain.catalog.SecurityProfile;
import com.cloudrangers.cloudpilot.repository.catalog.NetworkRepository;
import com.cloudrangers.cloudpilot.repository.catalog.SecurityProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeederNetworks implements CommandLineRunner {

    private final NetworkRepository networkRepository;
    private final SecurityProfileRepository securityProfileRepository;

    @Override
    public void run(String... args) {
        if (networkRepository.count() == 0) {
            networkRepository.save(NetworkSubnet.builder()
                    .id("subnet-0ab")
                    .name("frontend-subnet-a")
                    .cidr("10.0.10.0/24")
                    .zoneId(10L)
                    .providerId(1L)
                    .purposeCsv("frontend")
                    .build());
            networkRepository.save(NetworkSubnet.builder()
                    .id("pg-back-01")
                    .name("PG-Backend")
                    .cidr("172.16.20.0/24")
                    .zoneId(11L)
                    .providerId(2L)
                    .purposeCsv("backend")
                    .build());
        }

        if (securityProfileRepository.count() == 0) {
            securityProfileRepository.save(SecurityProfile.builder()
                    .id("sg-web")
                    .name("sg-web")
                    .providerId(1L)
                    .zoneId(10L)
                    .rulesCsv("80/tcp,443/tcp")
                    .build());
            securityProfileRepository.save(SecurityProfile.builder()
                    .id("nsx-db")
                    .name("nsx-db-seg")
                    .providerId(2L)
                    .zoneId(11L)
                    .rulesCsv("5432/tcp")
                    .build());
        }
    }
}
