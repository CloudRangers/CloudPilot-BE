package com.cloudrangers.cloudpilot.enums;

/**
 * pkg_request.status 와 1:1 매핑
 * DB enum: ('pending','l1_approved','approved','rejected')
 */
public enum PkgRequestStatus {
    pending,
    l1_approved,
    approved,
    rejected
}

