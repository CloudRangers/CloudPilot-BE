package com.cloudrangers.cloudpilot.service.provision;

import com.cloudrangers.cloudpilot.dto.message.ProvisionJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate; // Spring Boot JPA 사용 시 이미 들어옴

    @Value("${rabbitmq.exchange.provision.name:provision-exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routing-key.provision.base:provision.create}")
    private String baseRoutingKey;

    public void pushJob(ProvisionJobMessage msg, boolean highPriority) {
        // 0) 기본값 보정
        normalize(msg);

        // 1) 템플릿 Resolve (os_image)
        resolveTemplateFromOsImage(msg);

        // 2) 라우팅키
        final String routingKey = buildRoutingKey(msg);

        try {
            if (msg.getJobId() == null || msg.getJobId().isBlank()) {
                msg.setJobId(UUID.randomUUID().toString());
            }

            log.info("Publishing job: jobId={}, exchange={}, rk={}, provider={}, zone={}, template(item={}, moid={})",
                    msg.getJobId(), exchangeName, routingKey,
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
                        m.getMessageProperties().setContentType("application/json");
                        return m;
                    }
            );

        } catch (Exception e) {
            log.error("Rabbit publish failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String buildRoutingKey(ProvisionJobMessage msg) {
        String providerLower = String.valueOf(msg.getProviderType()).toLowerCase(Locale.ROOT);
        return baseRoutingKey + "." + providerLower; // e.g., provision.create.vsphere
    }

    // ---------- 내부 유틸 ----------

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

    /**
     * os_image에서 템플릿을 찾아 msg.template 채움.
     * - 1순위: code = "family-version-variant-arch"
     * - 2순위: family/version 토큰 LIKE
     *
     * 주의: DB zone_id는 SMALLINT → 쿼리 파라미터는 int로 전달
     * 메시지의 zoneId가 Long이든 Integer든 intValue()로 안전 변환
     */
    private void resolveTemplateFromOsImage(ProvisionJobMessage msg) {
        if (msg.getTemplate() != null
                && (!blank(msg.getTemplate().getItemName()) || !blank(msg.getTemplate().getTemplateMoid()))) {
            // 이미 채워져 있으면 그대로 사용
            return;
        }

        // ★ zoneId를 int로 통일
        Integer zoneIdInt = requireZoneIdInt(msg);

        String family  = safeLower(msg.getOs().getFamily());
        String version = safeLower(msg.getOs().getVersion());
        String variant = safeLower(msg.getOs().getVariant());
        String arch    = safeLower(msg.getOs().getArch());

        String code = (family + "-" + version + "-" + variant + "-" + arch);

        // 1) code 정확 매칭
        List<Map<String, Object>> exact = jdbcTemplate.queryForList("""
            SELECT id, code, name, template_name, template_moid, template_datastore, os_family, os_version, guest_id
              FROM os_image
             WHERE zone_id = ? AND is_active = TRUE AND LOWER(code) = ?
             ORDER BY id DESC LIMIT 1
        """, zoneIdInt, code.toLowerCase(Locale.ROOT));

        Map<String, Object> row = !exact.isEmpty() ? exact.get(0) : null;

        // 2) family/version 토큰 검색
        if (row == null) {
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

        if (row == null) {
            throw new IllegalArgumentException("OS 이미지 카탈로그를 찾을 수 없습니다. zone="
                    + zoneIdInt + ", os=" + family + " " + version + " (" + variant + "/" + arch + ")");
        }

        ProvisionJobMessage.TemplateRef t = new ProvisionJobMessage.TemplateRef();
        t.setItemName(         (String) row.get("template_name"));
        t.setTemplateMoid(     (String) row.get("template_moid"));
        t.setTemplateDatastore((String) row.get("template_datastore"));
        t.setGuestId(          (String) row.get("guest_id"));
        msg.setTemplate(t);

        // 참고: 추적 용도로 additionalConfig에 기록(선택)
        if (msg.getAdditionalConfig() == null) {
            msg.setAdditionalConfig(new LinkedHashMap<>());
        }
        msg.getAdditionalConfig().put("os_image_code", row.get("code"));
        msg.getAdditionalConfig().put("os_image_id",   row.get("id"));
    }

    private Integer requireZoneIdInt(ProvisionJobMessage msg) {
        // ProvisionJobMessage의 zoneId가 Long로 선언되어 있어도 int로 안전 변환
        Object z = msg.getZoneId(); // Long/Integer 모두 처리
        if (z == null) throw new IllegalArgumentException("zoneId is required");
        if (z instanceof Integer i) return i;
        if (z instanceof Long l)    return Math.toIntExact(l);
        // 혹시 문자열 등으로 올 경우 대비
        return Integer.parseInt(String.valueOf(z));
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }
}
