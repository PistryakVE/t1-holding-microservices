package org.example.aspect.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class IncomingHttpLogMessage {
    private Instant timestamp;
    private String methodSignature;
    private String uri;
    private String httpMethod;
    private Map<String, Object> parameters;
    private Object body;
    private Map<String, String> headers;
    private String remoteAddress;
}