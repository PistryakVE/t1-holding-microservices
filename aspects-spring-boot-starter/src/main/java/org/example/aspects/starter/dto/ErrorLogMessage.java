package org.example.aspects.starter.dto;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ErrorLogMessage {
    private Instant timestamp;
    private String serviceName;
    private String methodSignature;
    private String stackTrace;
    private String errorMessage;
    private String errorLevel;
    private List<Object> inputParameters;
    private String exceptionType;
}