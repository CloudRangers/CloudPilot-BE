package com.cloudrangers.cloudpilot.service.pkg;

import com.cloudrangers.cloudpilot.domain.pkg.AnsRun;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.message.InstallPackageJobMessage;
import com.cloudrangers.cloudpilot.dto.request.InstallPackagesRequest;
import com.cloudrangers.cloudpilot.repository.catalog.PackageVerRepository;
import com.cloudrangers.cloudpilot.repository.pkg.AnsRunRepository;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.cloudrangers.cloudpilot.security.CustomUserDetails;
import com.cloudrangers.cloudpilot.security.PermissionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageServiceImpl implements PackageService {

    private final RabbitTemplate rabbitTemplate;
    private final VmInstanceRepository vmInstanceRepository;
    private final PackageVerRepository packageVerRepository;
    private final AnsRunRepository ansRunRepository;
    private final PermissionChecker permissionChecker;

    @Transactional
    @Override
    public List<String> installPackages(CustomUserDetails userDetails, InstallPackagesRequest request) {

        Long userId = userDetails.getUserId();
        long now = System.currentTimeMillis();
        List<String> jobIds = new ArrayList<>();

        for (Long vmId : request.getVmIds()) {

            VmInstance vm = vmInstanceRepository.findById(vmId)
                    .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

            if (!permissionChecker.canInstallPackage(userId, vm.getTeamId(), vm.getOwnerUserId())) {
                throw new RuntimeException("No install permission for VM ID: " + vmId);
            }

            List<InstallPackageJobMessage.PackageItem> items = new ArrayList<>();

            for (InstallPackagesRequest.PkgItem pkg : request.getPackages()) {

                packageVerRepository
                        .findByPackageDefNameAndVersion(pkg.getName(), pkg.getVersion())
                        .orElseThrow(() ->
                                new RuntimeException("Package version not found: " + pkg.getName() + ":" + pkg.getVersion())
                        );

                items.add(
                        InstallPackageJobMessage.PackageItem.builder()
                                .name(pkg.getName())
                                .version(pkg.getVersion())
                                .build()
                );
            }

            AnsRun ansRun = ansRunRepository.save(
                    AnsRun.builder()
                            .vmInstance(vm)
                            .status(AnsRun.RunStatus.running)
                            .build()
            );

            String jobId = ansRun.getId().toString();
            jobIds.add(jobId);

            InstallPackageJobMessage msg = InstallPackageJobMessage.builder()
                    .jobId(jobId)
                    .ansRunId(ansRun.getId())
                    .vmId(vm.getId())
                    .hostname(vm.getName())
                    .ip(vm.getIp())
                    .requestedAt(now)
                    .requestedByUserId(userDetails.getUserId())
                    .requestedByEmpno(userDetails.getEmpno())
                    .requestedByUsername(userDetails.getUsername())
                    .requestedByRole(userDetails.getRoleCode())
                    .requestedByTeam(userDetails.getTeamName())
                    .packages(items)
                    .build();

            rabbitTemplate.convertAndSend(
                    "package-install-exchange",
                    "package.install",
                    msg
            );
        }

        return jobIds;
    }
}
