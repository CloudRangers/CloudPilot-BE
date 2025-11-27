package com.cloudrangers.cloudpilot.service.vm.producer;

import com.cloudrangers.cloudpilot.domain.vm.VmInstance;

public interface VmDeletionProducer {
    void enqueue(String jobId, VmInstance vmId, Long requestedBy);
}
