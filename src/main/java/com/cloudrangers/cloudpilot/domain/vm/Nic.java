package com.cloudrangers.cloudpilot.domain.vm;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NIC가 어느 VM에 속해있는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vm_instance_id", nullable = false)
    private VmInstance vmInstance;

    @Column(name = "private_ip")
    private String privateIp;
}





