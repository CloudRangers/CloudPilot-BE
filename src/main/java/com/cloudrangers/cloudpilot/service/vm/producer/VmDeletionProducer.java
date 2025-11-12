package com.cloudrangers.cloudpilot.service.vm.producer;

public interface VmDeletionProducer {
    void enqueue(String jobId, Long vmId, Long requestedBy);
}
