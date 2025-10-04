package org.example.aspects.starter.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class IncomingHttpLogMessage {
    private Instant timestamp;
    private String serviceName;
    private String methodSignature;
    private String uri;
    private String httpMethod;
    private String remoteAddress;
    private Map<String, Object> parameters;
    private Map<String, String> headers;
    private String body;
    private String logLevel;
}