package org.example.aspects.starter.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class MetricLogMessage {
    private Instant timestamp;
    private String serviceName;
    private String methodSignature;
    private long executionTimeMs;
    private List<Object> inputParameters;
    private String metricName;
    private String warningReason;
}