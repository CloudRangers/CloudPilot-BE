package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.domain.pipeline.TfRun;
import com.cloudrangers.cloudpilot.domain.provision.VmProvisionItem;
import com.cloudrangers.cloudpilot.domain.vm.VmInstance;
import com.cloudrangers.cloudpilot.dto.message.ProvisionJobMessage;
import com.cloudrangers.cloudpilot.enums.TfRunAction;
import com.cloudrangers.cloudpilot.enums.TfRunStatus;
import com.cloudrangers.cloudpilot.repository.pipeline.TfRunRepository;
import com.cloudrangers.cloudpilot.repository.provision.VmProvisionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final TfRunRepository tfRunRepository;
    private final VmProvisionItemRepository vmProvisionItemRepository;

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.provision.base:provision.create}")
    private String baseRoutingKey;

    // ========================================
    // ⭐ VM 생성 - TfRun 생성 + 큐 발행
    // ========================================
    @Transactional
    public void pushJob(ProvisionJobMessage msg, boolean highPriority) {
        // 0) 기본값 보정
        normalize(msg);

        // 1) 템플릿 Resolve (os_image)
        resolveTemplateFromOsImage(msg);

        // 2) TfRun 레코드 생성
        TfRun tfRun = createTfRun(msg);

        // 3) VmProvisionItem ↔ TfRun 연결 + additionalConfig에 tfRunId 주입
        Map<String, Object> add = msg.getAdditionalConfig();
        if (add == null) {
            add = new LinkedHashMap<>();
            msg.setAdditionalConfig(add);
        }

        Object itemIdObj = add.get("provisionItemId");
        if (itemIdObj != null) {
            Long provisionItemId = Long.parseLong(String.valueOf(itemIdObj));

            VmProvisionItem item = vmProvisionItemRepository.findById(provisionItemId)
                    .orElse(null);

            if (item != null) {
                item.setTfRunId(tfRun.getId());
                vmProvisionItemRepository.save(item);
                log.info("✓ VmProvisionItem linked to TfRun: itemId={}, tfRunId={}",
                        provisionItemId, tfRun.getId());
            }
        }

        // tfRunId를 메시지 additionalConfig에 항상 추가
        add.put("tfRunId", tfRun.getId());

        // 4) 라우팅키
        final String routingKey = buildRoutingKey(msg, "create");

        try {
            if (msg.getJobId() == null || msg.getJobId().isBlank()) {
                msg.setJobId(UUID.randomUUID().toString());
            }

            log.info("Publishing job: jobId={}, tfRunId={}, exchange={}, rk={}, provider={}, zone={}, template(item={}, moid={})",
                    msg.getJobId(), tfRun.getId(), exchangeName, routingKey,
                    msg.getProviderType(), msg.getZoneId(),
                    msg.getTemplate() != null ? msg.getTemplate().getItemName() : null,
                    msg.getTemplate() != null ? msg.getTemplate().getTemplateMoid() : null
            );

            rabbitTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    msg,
                    m -> {
                        m.getMessageProperties().setCorrelationId(msg.getJobId());
                        m.getMessageProperties().setHeader("jobId", msg.getJobId());
                        m.getMessageProperties().setHeader("tfRunId", tfRun.getId());  // 워커에서 사용
                        m.getMessageProperties().setContentType("application/json");
                        return m;
                    }
            );

        } catch (Exception e) {
            log.error("Rabbit publish failed: {}", e.getMessage(), e);

            // TfRun 실패 처리
            tfRun.setStatus(TfRunStatus.failed);
            tfRun.setFinishedAt(Instant.now());
            tfRunRepository.save(tfRun);

            throw e;
        }
    }

    // ========================================
    // ⭐ VM 삭제 - state_uri 조회 및 destroy Job 전송
    // ========================================

    /**
     * VM 삭제 Job을 RabbitMQ에 전송
     *
     * @param jobId       Job ID (UUID 등)
     * @param vm          삭제할 VM 인스턴스
     * @param requestedBy 요청자 User ID
     */
    @Transactional
    public void pushDeleteJob(String jobId, VmInstance vm, Long requestedBy) {
        try {
            // 1. 원본 TfRun 조회 (vm.tfRunId 우선, 레거시로 vm_provision_item 경유)
            TfRun originalTfRun = findOriginalTfRun(vm);

            // 2. destroy 실행에 사용할 stateUri 결정
            String stateUriForDestroy = null;
            if (originalTfRun != null && notBlank(originalTfRun.getStateUri())) {
                stateUriForDestroy = originalTfRun.getStateUri();
            } else if (notBlank(vm.getStateUri())) {
                // 신규 구조: vm_instance.state_uri 에 직접 저장된 값 사용
                stateUriForDestroy = vm.getStateUri();
            }

            if (stateUriForDestroy == null) {
                log.warn("⚠️ No stateUri found for VM deletion: vmId={}, vmName={}",
                        vm.getId(), vm.getName());
            } else {
                log.info("✓ stateUri resolved for destroy: vmId={}, stateUri={}",
                        vm.getId(), stateUriForDestroy);
            }

            // 3. 새 TfRun 레코드 생성 (destroy용)
            TfRun destroyTfRun = TfRun.builder()
                    .workspace(vm.getName())
                    .action(TfRunAction.destroy)
                    .status(TfRunStatus.running)
                    .stateBackend("local")
                    .startedAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            destroyTfRun = tfRunRepository.save(destroyTfRun);
            final Long finalTfRunId = destroyTfRun.getId();

            log.info("✓ TfRun created for destroy: id={}, workspace={}",
                    finalTfRunId, destroyTfRun.getWorkspace());

            // 4. ProvisionJobMessage 생성 (stateUri 포함)
            ProvisionJobMessage msg = buildDestroyMessage(
                    jobId,
                    vm,
                    requestedBy,
                    finalTfRunId,
                    stateUriForDestroy
            );

            // 5. 라우팅 키 생성 (provision.destroy.vsphere)
            String routingKey = buildRoutingKey(msg, "destroy");

            log.info("📤 Publishing destroy job: jobId={}, tfRunId={}, vmId={}, vmName={}, stateUri={}, exchange={}, routingKey={}",
                    jobId, finalTfRunId, vm.getId(), vm.getName(),
                    stateUriForDestroy != null ? stateUriForDestroy : "null",
                    exchangeName, routingKey);

            // 6. RabbitMQ로 전송
            rabbitTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    msg,
                    m -> {
                        m.getMessageProperties().setCorrelationId(jobId);
                        m.getMessageProperties().setHeader("jobId", jobId);
                        m.getMessageProperties().setHeader("tfRunId", finalTfRunId);
                        m.getMessageProperties().setHeader("action", "destroy");
                        m.getMessageProperties().setContentType("application/json");
                        return m;
                    }
            );

            log.info("✅ Destroy job published successfully: jobId={}, tfRunId={}",
                    jobId, finalTfRunId);

        } catch (Exception e) {
            log.error("❌ Failed to publish destroy job: jobId={}, vmId={}",
                    jobId, vm.getId(), e);
            throw new RuntimeException("Failed to enqueue VM deletion job", e);
        }
    }

    // ========================================
    // TfRun 관련 헬퍼
    // ========================================

    /**
     * VM 생성 시 TfRun 레코드 생성
     */
    private TfRun createTfRun(ProvisionJobMessage msg) {
        TfRun tfRun = TfRun.builder()
                .workspace(msg.getVmName())
                .action(TfRunAction.apply)
                .status(TfRunStatus.running)
                .stateBackend("local")
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        tfRun = tfRunRepository.save(tfRun);

        log.info("✓ TfRun created: id={}, workspace={}, action={}",
                tfRun.getId(), tfRun.getWorkspace(), tfRun.getAction());

        return tfRun;
    }

    /**
     * VM 삭제 시 원본 TfRun 조회
     *
     * 우선순위:
     *  1) vm.tfRunId → tf_run
     *  2) (레거시) vm.provisionItemId → vm_provision_item → tf_run
     */
    private TfRun findOriginalTfRun(VmInstance vm) {
        // 1) 신규 구조: vm_instance.tf_run_id 직접 사용
        if (vm.getTfRunId() != null) {
            TfRun tfRun = tfRunRepository.findById(vm.getTfRunId()).orElse(null);
            if (tfRun != null) {
                log.info("✓ Found original TfRun via vm.tfRunId: vmId={}, tfRunId={}, stateUri={}",
                        vm.getId(), tfRun.getId(), tfRun.getStateUri());
                return tfRun;
            } else {
                log.warn("⚠️ VmInstance.tfRunId is set but TfRun not found: vmId={}, tfRunId={}",
                        vm.getId(), vm.getTfRunId());
            }
        }

        // 2) 레거시 구조: vm_instance.provision_item_id → vm_provision_item.tf_run_id
        Long provisionItemId = vm.getProvisionItemId();

        if (provisionItemId == null) {
            log.warn("⚠️ VM has no provisionItemId: vmId={}", vm.getId());
            return null;
        }

        VmProvisionItem item = vmProvisionItemRepository.findById(provisionItemId)
                .orElse(null);

        if (item == null) {
            log.warn("⚠️ VmProvisionItem not found: itemId={}", provisionItemId);
            return null;
        }

        if (item.getTfRunId() == null) {
            log.warn("⚠️ VmProvisionItem has no tfRunId: itemId={}", provisionItemId);
            return null;
        }

        TfRun tfRun = tfRunRepository.findById(item.getTfRunId())
                .orElse(null);

        if (tfRun == null) {
            log.warn("⚠️ TfRun not found: tfRunId={}", item.getTfRunId());
            return null;
        }

        log.info("✓ Found original TfRun via VmProvisionItem: vmId={}, tfRunId={}, stateUri={}",
                vm.getId(), tfRun.getId(), tfRun.getStateUri());

        return tfRun;
    }

    // ========================================
    // 공통 헬퍼 메서드
    // ========================================

    /**
     * action에 따라 라우팅 키 생성
     * - create  → provision.create.vsphere
     * - destroy → provision.destroy.vsphere
     */
    private String buildRoutingKey(ProvisionJobMessage msg, String action) {
        String providerLower = String.valueOf(msg.getProviderType()).toLowerCase(Locale.ROOT);

        // baseRoutingKey = "provision.create"
        // → "provision" + "." + action + "." + provider
        String baseWithoutAction = baseRoutingKey.replace(".create", "");
        return baseWithoutAction + "." + action + "." + providerLower;
    }

    /**
     * Terraform destroy를 위한 ProvisionJobMessage 생성
     */
    private ProvisionJobMessage buildDestroyMessage(
            String jobId,
            VmInstance vm,
            Long requestedBy,
            Long tfRunId,
            String stateUri
    ) {
        ProvisionJobMessage msg = new ProvisionJobMessage();

        // 기본 정보
        msg.setJobId(jobId);
        msg.setAction("destroy"); // Terraform destroy
        msg.setProviderType(vm.getProviderType());
        msg.setZoneId(vm.getZoneId() != null ? vm.getZoneId().longValue() : null);

        // VM 정보
        msg.setVmName(vm.getName());
        msg.setVmCount(1);  // 삭제는 항상 단일 VM

        // 리소스 스펙 (참고용)
        msg.setCpuCores(vm.getVcpu());
        msg.setMemoryGb(vm.getMemoryMb() != null ? vm.getMemoryMb() / 1024 : null);
        msg.setDiskGb(vm.getRootDiskGb());

        // 사용자 컨텍스트
        msg.setUserId(requestedBy);
        msg.setTeamId(vm.getTeamId());

        // 추가 설정 (Terraform State URI 전달)
        Map<String, Object> additionalConfig = new LinkedHashMap<>();
        additionalConfig.put("vmId", vm.getId());
        additionalConfig.put("operation", "destroy");
        additionalConfig.put("requestedBy", requestedBy);
        additionalConfig.put("tfRunId", tfRunId);

        if (stateUri != null && !stateUri.isBlank()) {
            additionalConfig.put("stateUri", stateUri);  // 핵심: 기존 state 재사용
            log.info("✓ State URI added to destroy message: {}", stateUri);
        } else {
            log.warn("⚠️ No state URI available for destroy operation");
        }

        msg.setAdditionalConfig(additionalConfig);

        log.debug("🔨 Built destroy message: jobId={}, vmName={}, action={}, tfRunId={}",
                jobId, vm.getName(), msg.getAction(), tfRunId);

        return msg;
    }

    // ========================================
    // 기존 메서드들 (기본값/템플릿 Resolve)
    // ========================================

    private void normalize(ProvisionJobMessage msg) {
        if (msg.getJobId() == null || msg.getJobId().isBlank()) {
            msg.setJobId(UUID.randomUUID().toString());
        }
        // OS 기본
        if (msg.getOs() == null) {
            var os = new ProvisionJobMessage.OsSpec();
            os.setFamily("ubuntu");
            os.setVersion("22.04");
            os.setVariant("minimal");
            os.setArch("x86_64");
            msg.setOs(os);
        } else {
            if (blank(msg.getOs().getVariant())) msg.getOs().setVariant("minimal");
            if (blank(msg.getOs().getArch()))    msg.getOs().setArch("x86_64");
        }
        // NET 기본
        if (msg.getNet() == null) {
            var net = new ProvisionJobMessage.NetSpec();
            net.setMode("DHCP");
            msg.setNet(net);
        } else if (blank(msg.getNet().getMode())) {
            msg.getNet().setMode("DHCP");
        }
        // PROPERTIES 기본
        if (msg.getProperties() == null) {
            var p = new ProvisionJobMessage.PropertiesSpec();
            p.setHostname(msg.getVmName());
            p.setTimezone("Asia/Seoul");
            msg.setProperties(p);
        } else {
            if (blank(msg.getProperties().getHostname())) msg.getProperties().setHostname(msg.getVmName());
            if (blank(msg.getProperties().getTimezone())) msg.getProperties().setTimezone("Asia/Seoul");
        }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    /**
     * os_image / template_name 기반으로 템플릿 및 OS 이미지 코드 resolve
     */
    private void resolveTemplateFromOsImage(ProvisionJobMessage msg) {
        // additionalConfig 보정
        Map<String, Object> add = msg.getAdditionalConfig();
        if (add == null) {
            add = new LinkedHashMap<>();
            msg.setAdditionalConfig(add);
        }

        // 이미 os_image_code 가 있다면 그대로 사용
        if (add.get("os_image_code") != null) {
            return;
        }

        Integer zoneIdInt = requireZoneIdInt(msg);
        Map<String, Object> row = null;

        // 1) templateName 기반으로 os_image 찾기
        String templateName = null;

        // additionalConfig.templateName 우선
        Object templateNameObj = add.get("templateName");
        if (templateNameObj != null && !blank(String.valueOf(templateNameObj))) {
            templateName = String.valueOf(templateNameObj).trim();
        }

        // msg.getTemplate().itemName 보조로 사용
        if (blank(templateName) && msg.getTemplate() != null && !blank(msg.getTemplate().getItemName())) {
            templateName = msg.getTemplate().getItemName().trim();
        }

        if (!blank(templateName)) {
            // template_name 정확 일치 우선
            List<Map<String, Object>> byTemplate = jdbcTemplate.queryForList("""
                SELECT id, code, name, template_name, template_moid, template_datastore,
                       os_family, os_version, guest_id
                  FROM os_image
                 WHERE zone_id = ? AND is_active = TRUE AND template_name = ?
                 ORDER BY id DESC LIMIT 1
            """, zoneIdInt, templateName);

            if (!byTemplate.isEmpty()) {
                row = byTemplate.get(0);
                log.info("[JobQueueService] os_image resolved by template_name. zoneId={}, templateName={}, osImageId={}, code={}",
                        zoneIdInt, templateName, row.get("id"), row.get("code"));
            } else {
                // 혹시 경로가 살짝 다를 수 있으니 LIKE도 한 번 더 시도
                String like = "%" + templateName + "%";
                List<Map<String, Object>> likeRows = jdbcTemplate.queryForList("""
                    SELECT id, code, name, template_name, template_moid, template_datastore,
                           os_family, os_version, guest_id
                      FROM os_image
                     WHERE zone_id = ? AND is_active = TRUE AND template_name LIKE ?
                     ORDER BY id DESC LIMIT 1
                """, zoneIdInt, like);
                if (!likeRows.isEmpty()) {
                    row = likeRows.get(0);
                    log.info("[JobQueueService] os_image resolved by template_name LIKE. zoneId={}, templateName={}, matchedTemplateName={}, osImageId={}, code={}",
                            zoneIdInt, templateName, row.get("template_name"), row.get("id"), row.get("code"));
                }
            }
        }

        // 2) template_name 으로 못 찾았으면 기존 OS 스펙 기반 조회 (fallback)
        if (row == null) {
            String family  = safeLower(msg.getOs().getFamily());
            String version = safeLower(msg.getOs().getVersion());
            String variant = safeLower(msg.getOs().getVariant());
            String arch    = safeLower(msg.getOs().getArch());

            String code = (family + "-" + version + "-" + variant + "-" + arch);

            List<Map<String, Object>> exact = jdbcTemplate.queryForList("""
                SELECT id, code, name, template_name, template_moid, template_datastore, os_family, os_version, guest_id
                  FROM os_image
                 WHERE zone_id = ? AND is_active = TRUE AND LOWER(code) = ?
                 ORDER BY id DESC LIMIT 1
            """, zoneIdInt, code.toLowerCase(Locale.ROOT));

            if (!exact.isEmpty()) {
                row = exact.get(0);
            } else {
                String famLike = "%" + family + "%";
                String verLike = "%" + version + "%";
                List<Map<String, Object>> cands = jdbcTemplate.queryForList("""
                    SELECT id, code, name, template_name, template_moid, template_datastore, os_family, os_version, guest_id
                      FROM os_image
                     WHERE zone_id = ? AND is_active = TRUE
                       AND (LOWER(os_family) LIKE ? OR LOWER(name) LIKE ? OR LOWER(code) LIKE ?)
                       AND (LOWER(os_version) LIKE ? OR LOWER(name) LIKE ? OR LOWER(code) LIKE ?)
                     ORDER BY id DESC LIMIT 1
                """, zoneIdInt, famLike, famLike, famLike, verLike, verLike, verLike);
                if (!cands.isEmpty()) row = cands.get(0);
            }

            if (row != null) {
                log.info("[JobQueueService] os_image resolved by OS spec. zoneId={}, family={}, version={}, variant={}, arch={}, osImageId={}, code={}",
                        zoneIdInt, family, version, variant, arch, row.get("id"), row.get("code"));
            }
        }

        if (row == null) {
            throw new IllegalArgumentException("OS 이미지 카탈로그를 찾을 수 없습니다. zone="
                    + zoneIdInt + ", templateName=" + templateName);
        }

        // 3) TemplateRef 세팅 (없으면 새로 만들고, 비어 있는 필드만 채우기)
        ProvisionJobMessage.TemplateRef t = msg.getTemplate();
        if (t == null) {
            t = new ProvisionJobMessage.TemplateRef();
        }
        if (blank(t.getItemName())) {
            t.setItemName((String) row.get("template_name"));
        }
        if (blank(t.getTemplateMoid())) {
            t.setTemplateMoid((String) row.get("template_moid"));
        }
        if (blank(t.getTemplateDatastore())) {
            t.setTemplateDatastore((String) row.get("template_datastore"));
        }
        if (blank(t.getGuestId())) {
            t.setGuestId((String) row.get("guest_id"));
        }
        msg.setTemplate(t);

        // 4) os_image_code / os_image_id 를 additionalConfig 에 심어서 Worker 로 전달
        add.put("os_image_code", row.get("code"));
        add.put("os_image_id", row.get("id"));
    }

    private Integer requireZoneIdInt(ProvisionJobMessage msg) {
        Object z = msg.getZoneId();
        if (z == null) throw new IllegalArgumentException("zoneId is required");
        if (z instanceof Integer i) return i;
        if (z instanceof Long l)    return Math.toIntExact(l);
        return Integer.parseInt(String.valueOf(z));
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }
}
