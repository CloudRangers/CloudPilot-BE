package com.cloudrangers.cloudpilot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesPoint {

    private Instant timestamp;
    private double value;
}
