package com.cloudrangers.cloudpilot.enums;

import lombok.Getter;

@Getter
public enum ProvisionStatus {
    QUEUED("대기 중", "프로비저닝 작업이 큐에 대기 중입니다"),
    RUNNING("실행 중", "프로비저닝 작업이 실행 중입니다"),
    SUCCEEDED("성공", "프로비저닝 작업이 성공적으로 완료되었습니다"),
    FAILED("실패", "프로비저닝 작업이 실패했습니다"),
    CANCELLED("취소됨", "프로비저닝 작업이 취소되었습니다");

    private final String displayName;
    private final String description;

    ProvisionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }

    public boolean isInProgress() {
        return this == QUEUED || this == RUNNING;
    }
}
