package com.cloudrangers.cloudpilot.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "provider")
public class Provider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "name")
    private String name;

    @Column(name = "account_id")
    private String accountId;
}
