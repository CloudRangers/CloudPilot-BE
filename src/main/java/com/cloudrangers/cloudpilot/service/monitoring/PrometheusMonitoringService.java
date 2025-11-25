package com.cloudrangers.cloudpilot.service.monitoring;

import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusQueryResponse;
import com.cloudrangers.cloudpilot.dto.monitoring.PrometheusSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class PrometheusMonitoringService {

    @Value("${monitoring.prometheus.base-url}")
    private String prometheusBaseUrl;

    @Value("${monitoring.prometheus.up-query:up}")
    private String upQuery;

    /**
     * Prometheus up 메트릭을 조회해서
     * - 전체 타겟 수
     * - up(1)인 타겟 수
     * - down(0 또는 그 외) 타겟 수
     * 를 계산해서 반환
     */
    public PrometheusSummaryResponse getUpSummary() {
        String url = prometheusBaseUrl + "/api/v1/query?query=" + upQuery;
        log.info("Calling Prometheus: {}", url);

        RestTemplate restTemplate = new RestTemplate(); // 팀에 RestTemplate Bean 있으면 그걸 @Autowired로 써도 됨

        ResponseEntity<PrometheusQueryResponse> response =
                restTemplate.getForEntity(url, PrometheusQueryResponse.class);

        PrometheusQueryResponse body = response.getBody();
        if (body == null || body.getData() == null || body.getData().getResult() == null) {
            log.warn("Prometheus response is null or invalid");
            return new PrometheusSummaryResponse(0, 0, 0);
        }

        List<PrometheusQueryResponse.Result> results = body.getData().getResult();

        int total = results.size();
        int up = 0;

        for (PrometheusQueryResponse.Result r : results) {
            List<Object> value = r.getValue();
            // value 예: [ 1719999999.123, "1" ]
            if (value != null && value.size() >= 2) {
                Object v = value.get(1);
                if (v != null && "1".equals(v.toString())) {
                    up++;
                }
            }
        }

        int down = total - up;
        return new PrometheusSummaryResponse(total, up, down);
    }
}
