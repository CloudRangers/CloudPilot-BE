package com.cloudrangers.cloudpilot.dto.message;

import com.cloudrangers.cloudpilot.dto.request.ProvisionRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProvisionJobMessage {

    // ===== 공통/식별 =====
    private String jobId;
    private Object providerType;     // "VSPHERE" 등 (Enum/문자열 허용)
    private Integer zoneId;

    // ===== 호출자 컨텍스트(워커 로그/추적용) =====
    private Long userId;
    private Long teamId;

    // ===== VM 스펙(워커/TerraformExecutor가 참조) =====
    private Integer vmCount;
    private String  vmName;
    private Integer cpuCores;
    private Integer memoryGb;
    private Integer diskGb;

    private Map<String, String>  tags;
    private Map<String, Object>  additionalConfig; // { imageType, imageName, imagePath ... }

    // ===== vSphere 오버라이드(없으면 워커 ProviderCredentials가 기본값 사용) =====
    private String datacenter;  // 선택
    private String cluster;     // 선택
    private String datastore;   // 선택
    private String folder;      // 선택
    private String network;     // 선택

    // ===== 네트워킹/AWS 호환(워커 로그용; vSphere면 비워도 됨) =====
    private String vpcId;
    private String subnetId;
    private String securityGroupId;

    // ===== API 원본 요청 스냅샷(재시도/감사용) =====
    private ProvisionRequest request;  // 워커 DTO에 없더라도 JSON 역직렬화 시 무시됨

    // ===== 워크플로 액션 =====
    private String action; // "apply" | "destroy" 등

    // ===== CL/OS/초기화(앞으로 확장용; 워커는 unknown 무시) =====
    private TemplateRef    template;      // CL 템플릿 메타
    private OsSpec         os;            // OS 선택
    private NetSpec        net;           // DHCP/STATIC
    private PropertiesSpec properties;    // cloud-init/Windows customize

    // ---------- Nested Types ----------
    @Data
    public static class TemplateRef {
        private String itemName;           // os_image.template_name
        private String templateMoid;       // os_image.template_moid
        private String templateDatastore;  // os_image.template_datastore
        private String guestId;            // os_image.guest_id
        private String contentLibraryName; // 선택
    }

    @Data
    public static class OsSpec {
        private String family;   // ubuntu | rocky | windows
        private String version;  // "22.04"
        private String variant;  // "minimal"
        private String arch;     // x86_64
    }

    @Data
    public static class NetSpec {
        private String mode;     // DHCP | STATIC
        private String iface;    // ens192 (선택)
        private Ipv4 ipv4;       // STATIC일 때만
        private List<String> dns;

        @Data
        public static class Ipv4 {
            private String address;
            private Integer prefix;
            private String gateway;
        }
    }

    @Data
    public static class PropertiesSpec {
        private String hostname;
        private String timezone;
        private List<User> users;
        private List<String> packages;
        private List<FileSpec> files;
        private List<String> runcmd;
        private WinSpec win; // windows 전용

        @Data
        public static class User {
            private String name;
            private String sudo;
            private String shell;
            private List<String> ssh_authorized_keys;
        }

        @Data
        public static class FileSpec {
            private String path;
            private String content;
            private String owner;
            private String perm;
        }

        @Data
        public static class WinSpec {
            private String admin_password;
            private String join_domain;
            private String domain_admin_user;
            private String domain_admin_password;
            private List<String> firstboot_ps;
        }
    }
}
