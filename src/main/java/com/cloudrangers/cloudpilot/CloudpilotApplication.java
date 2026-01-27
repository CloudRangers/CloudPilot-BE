package com.cloudrangers.cloudpilot;

import com.cloudrangers.cloudpilot.config.VcenterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(VcenterProperties.class)
public class CloudpilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudpilotApplication.class, args);
    }
}
