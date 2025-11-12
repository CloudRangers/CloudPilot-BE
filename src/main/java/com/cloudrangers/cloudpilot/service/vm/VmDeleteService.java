package com.cloudrangers.cloudpilot.service.vm;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.response.DeleteVmResponse;
import com.cloudrangers.cloudpilot.repository.vm.VmInstanceRepository;
import com.cloudrangers.cloudpilot.service.vm.producer.VmDeletionProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VmDeleteService {

    private final VmInstanceRepository vmInstanceRepository;
    private final VmDeletionProducer deletionProducer;

    public DeleteVmResponse enqueueDeletion(Long vmId, Long requestedBy) {
        VmInstance vm = vmInstanceRepository.findById(vmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VM not found: " + vmId));

        String jobId = UUID.randomUUID().toString();
        // 실제로는 MQ/Kafka/SQS 등에 전송
        deletionProducer.enqueue(jobId, vm.getId(), requestedBy);

        return DeleteVmResponse.builder()
                .jobId(jobId)
                .vmId(vm.getId())
                .status("QUEUED")
                .requestedBy(requestedBy)
                .build();
    }
}
