package com.cloudrangers.cloudpilot.dto.vcenter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VCenterHostListResponse {
    private List<VCenterHostSummary> value;
}
