package com.cloudrangers.cloudpilot.dto.monitoring;

/**
 * FE vm-status 대시보드에서 쓸 Prometheus 요약 정보
 */
public class PrometheusSummaryResponse {

    private int totalTargets;
    private int upTargets;
    private int downTargets;

    public PrometheusSummaryResponse(int totalTargets, int upTargets, int downTargets) {
        this.totalTargets = totalTargets;
        this.upTargets = upTargets;
        this.downTargets = downTargets;
    }

    public int getTotalTargets() {
        return totalTargets;
    }

    public int getUpTargets() {
        return upTargets;
    }

    public int getDownTargets() {
        return downTargets;
    }
}
