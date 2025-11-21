package com.cloudrangers.cloudpilot.dto.message;

import java.time.OffsetDateTime;
import java.util.List;

public class ProvisionResultMessage {

    // 어떤 종류의 이벤트인지 구분
    public enum EventType {
        LOG,        // 테라폼 진행 로그
        SUCCESS,    // VM 생성 완료
        ERROR       // 비즈니스 에러 (terraform 실패 등)
    }

    private String jobId;          // job 테이블 PK(Long)를 문자열로 전달
    private EventType eventType;   // LOG / SUCCESS / ERROR

    // 상태 문자열 (호환용: RUNNING/SUCCEEDED/FAILED 등 자유)
    private String status;

    // 단일 VM 식별자(필요하면 첫 번째 VM 이름 등)
    private String vmId;

    // 로그/에러/요약 메시지
    private String message;

    // 공통 메타데이터
    private String step;           // "terraform_init", "terraform_apply" 등
    private OffsetDateTime timestamp;

    // SUCCESS일 때만 채우는 VM 정보 (여러 개 가능)
    private List<InstanceInfo> instances;

    public ProvisionResultMessage() {}

    // 예전 코드 호환용 생성자
    public ProvisionResultMessage(String jobId, String status, String vmId, String message) {
        this.jobId = jobId;
        this.status = status;
        this.vmId = vmId;
        this.message = message;
    }

    // === getter / setter ===

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVmId() { return vmId; }
    public void setVmId(String vmId) { this.vmId = vmId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public List<InstanceInfo> getInstances() { return instances; }
    public void setInstances(List<InstanceInfo> instances) { this.instances = instances; }

    @Override
    public String toString() {
        return "ProvisionResultMessage{" +
                "jobId='" + jobId + '\'' +
                ", eventType=" + eventType +
                ", status='" + status + '\'' +
                ", vmId='" + vmId + '\'' +
                ", message='" + message + '\'' +
                ", step='" + step + '\'' +
                ", timestamp=" + timestamp +
                ", instances=" + instances +
                '}';
    }

    // VM 한 개에 대한 정보 (워커에서 채워서 보내줌)
    public static class InstanceInfo {

        private String name;
        private String externalId;   // vSphere vm-123 이런 기본 ID
        private Long zoneId;
        private String providerType;
        private Integer cpuCores;
        private Integer memoryGb;
        private Integer diskGb;
        private String ipAddress;
        private String osType;

        // ⭐ nic 주소들: "172.16.0.10,172.16.0.11" 이런 식의 콤마 구분 문자열
        private String nicAddresses;

        public InstanceInfo() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }

        public Long getZoneId() { return zoneId; }
        public void setZoneId(Long zoneId) { this.zoneId = zoneId; }

        public String getProviderType() { return providerType; }
        public void setProviderType(String providerType) { this.providerType = providerType; }

        public Integer getCpuCores() { return cpuCores; }
        public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }

        public Integer getMemoryGb() { return memoryGb; }
        public void setMemoryGb(Integer memoryGb) { this.memoryGb = memoryGb; }

        public Integer getDiskGb() { return diskGb; }
        public void setDiskGb(Integer diskGb) { this.diskGb = diskGb; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getOsType() { return osType; }
        public void setOsType(String osType) { this.osType = osType; }

        public String getNicAddresses() { return nicAddresses; }
        public void setNicAddresses(String nicAddresses) { this.nicAddresses = nicAddresses; }

        @Override
        public String toString() {
            return "InstanceInfo{" +
                    "name='" + name + '\'' +
                    ", externalId='" + externalId + '\'' +
                    ", zoneId=" + zoneId +
                    ", providerType='" + providerType + '\'' +
                    ", cpuCores=" + cpuCores +
                    ", memoryGb=" + memoryGb +
                    ", diskGb=" + diskGb +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", osType='" + osType + '\'' +
                    ", nicAddresses='" + nicAddresses + '\'' +
                    '}';
        }
    }
}
