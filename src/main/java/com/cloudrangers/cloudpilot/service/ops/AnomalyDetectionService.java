package com.cloudrangers.cloudpilot.service.ops;

import com.cloudrangers.cloudpilot.dto.ops.AnomalyDetectionRequest;
import com.cloudrangers.cloudpilot.dto.ops.AnomalyDetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    // TODO: 나중에 AI 모델이나 외부 서비스 연동 시 여기 의존성 주입
    // private final AiModelClient aiModelClient;

    public AnomalyDetectionResult analyze(AnomalyDetectionRequest request) {
        log.info("[AnomalyDetection] 요청: vmId={}, metrics={}",
                request.getVmId(), request.getMetricNames());

        // 🔹 지금은 간단한 더미 로직 (AI 연결 전까지)
        // CPU 같은 메트릭 평균이 0.8 이상이면 이상으로 가정
        boolean anomaly = false;
        double score = 0.0;

        if (request.getMetrics() != null && !request.getMetrics().isEmpty()) {
            // 대충 아무 메트릭 하나 꺼내서 평균 계산 (임시)
            var firstEntry = request.getMetrics().entrySet().iterator().next();
            var values = firstEntry.getValue();

            if (values != null && !values.isEmpty()) {
                double avg = values.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                score = avg; // 그냥 예시로 평균값을 score로 사용
                anomaly = avg > 0.8;
            }
        }

        String severity = anomaly ? "critical" : "normal";

        return AnomalyDetectionResult.builder()
                .vmId(request.getVmId())
                .anomaly(anomaly)
                .score(score)
                .severity(severity)
                .summary(anomaly ? "이상 징후 감지됨" : "정상 상태로 판단됨")
                .explanation("※ 현재는 임시 휴리스틱 로직이며, 추후 AI 모델로 대체 예정")
                .build();
    }
}
