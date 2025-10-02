package org.example.aspect.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
public class OutgoingHttpLogMessage {
    private Instant timestamp;
    private String methodSignature;
    private String uri;
    private Map<String, Object> parameters;
    private Object body;
    private String httpMethod;
    private Map<String, String> headers;
}