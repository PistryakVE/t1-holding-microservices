package org.example.aspects.starter.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class OutgoingHttpLogMessage {
    private Instant timestamp;
    private String serviceName;
    private String methodSignature;
    private String uri;
    private String httpMethod;
    private Map<String, String> headers;
    private Object body;
    private String logLevel;
}