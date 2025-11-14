package com.cloudrangers.cloudpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class CloudpilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudpilotApplication.class, args);

    }
}
