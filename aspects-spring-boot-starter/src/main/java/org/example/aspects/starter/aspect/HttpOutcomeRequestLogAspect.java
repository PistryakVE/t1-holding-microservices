package org.example.aspects.starter.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aspects.starter.annotation.HttpOutcomeRequestLog;
import org.example.aspects.starter.config.AspectsProperties;
import org.example.aspects.starter.dto.OutgoingHttpLogMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Aspect
public class HttpOutcomeRequestLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(HttpOutcomeRequestLogAspect.class);

    private final AspectsProperties.HttpRequest properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private String serviceName;

    public HttpOutcomeRequestLogAspect(AspectsProperties.HttpRequest properties,
                                       KafkaTemplate<String, Object> kafkaTemplate,
                                       ObjectMapper objectMapper,
                                       @Value("${spring.application.name:unknown-service}") String serviceName) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;

        logger.info("=== HTTP OUTCOME REQUEST LOG ASPECT INITIALIZED ===");
        logger.info("Service: {}, Enabled: {}", serviceName, properties.isEnabled());
    }

    @Pointcut("@annotation(org.example.aspects.starter.annotation.HttpOutcomeRequestLog)")
    public void annotatedMethod() {}

    @AfterReturning(pointcut = "annotatedMethod()", returning = "result")
    public void logOutgoingHttpRequest(JoinPoint joinPoint, Object result) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            logger.debug("=== OUTGOING HTTP REQUEST ASPECT TRIGGERED ===");
            logger.debug("Method: {}", joinPoint.getSignature().toShortString());

            OutgoingHttpLogMessage logMessage = createOutgoingLogMessage(joinPoint, result);

            boolean kafkaSuccess = sendToKafka(logMessage);

            if (!kafkaSuccess && properties.isFallbackToDatabase()) {
                logger.info("Kafka failed for outgoing HTTP log - would save to database");
                handleDatabaseFallback(logMessage, "Kafka unavailable");
            }

        } catch (Exception e) {
            logger.error("Failed to log outgoing HTTP request: {}", e.getMessage());
        }
    }

    private OutgoingHttpLogMessage createOutgoingLogMessage(JoinPoint joinPoint, Object result) {
        OutgoingHttpLogMessage logMessage = new OutgoingHttpLogMessage();
        logMessage.setTimestamp(Instant.now());
        logMessage.setServiceName(serviceName);
        logMessage.setMethodSignature(joinPoint.getSignature().toShortString());
        logMessage.setLogLevel("INFO");

        Object[] args = joinPoint.getArgs();
        extractHttpInfo(args, logMessage);

        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            Map<String, Object> responseInfo = new HashMap<>();
            responseInfo.put("statusCode", response.getStatusCodeValue());
            responseInfo.put("statusText", response.getStatusCode().toString());
            responseInfo.put("body", response.getBody());
            logMessage.setBody(responseInfo);
        }

        return logMessage;
    }

    private void extractHttpInfo(Object[] args, OutgoingHttpLogMessage logMessage) {
        for (Object arg : args) {
            if (arg instanceof String && ((String) arg).startsWith("http")) {
                logMessage.setUri((String) arg);
                if (logMessage.getHttpMethod() == null) {
                    logMessage.setHttpMethod("GET");
                }
            } else if (arg instanceof URI) {
                logMessage.setUri(arg.toString());
                if (logMessage.getHttpMethod() == null) {
                    logMessage.setHttpMethod("GET");
                }
            } else if (arg instanceof HttpEntity) {
                HttpEntity<?> entity = (HttpEntity<?>) arg;
                logMessage.setBody(entity.getBody());

                if (entity.getHeaders() != null) {
                    Map<String, String> headers = new HashMap<>();
                    entity.getHeaders().forEach((key, values) -> {
                        if (values != null && !values.isEmpty()) {
                            headers.put(key, String.join(", ", values));
                        }
                    });
                    logMessage.setHeaders(headers);
                }
            } else if (arg instanceof HttpMethod) {
                logMessage.setHttpMethod(((HttpMethod) arg).name());
            }
        }

        if (logMessage.getUri() == null) {
            for (Object arg : args) {
                if (arg != null && arg.toString().contains("http")) {
                    logMessage.setUri(arg.toString());
                    break;
                }
            }
        }
    }

    private boolean sendToKafka(OutgoingHttpLogMessage logMessage) {
        if (!properties.isKafkaEnabled()) {
            logger.debug("Kafka disabled in configuration for outgoing HTTP request logging");
            return false;
        }

        try {
            logger.debug("Sending outgoing HTTP request log to Kafka topic: {}", properties.getKafkaTopic());

            // Преобразуем в JSON строку
            String jsonPayload = objectMapper.writeValueAsString(logMessage);

            var message = MessageBuilder
                    .withPayload(jsonPayload) // ← Отправляем как строку
                    .setHeader(KafkaHeaders.TOPIC, properties.getKafkaTopic())
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", "INFO")
                    .setHeader("logType", "HTTP_OUTGOING_REQUEST")
                    .build();

            var result = kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);

            logger.info("Outgoing HTTP log sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;

        } catch (Exception e) {
            logger.error("Kafka send failed for outgoing HTTP log: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Абстрактный метод для переопределения в микросервисах
     */
    protected void handleDatabaseFallback(OutgoingHttpLogMessage logMessage, String kafkaError) {
        logger.warn("DATABASE FALLBACK REQUIRED for outgoing HTTP log - Service: {}, URI: {}, Kafka Error: {}",
                logMessage.getServiceName(),
                logMessage.getUri(),
                kafkaError);

        logger.info("Override handleDatabaseFallback method in your microservice to save outgoing HTTP logs to database");
    }
}