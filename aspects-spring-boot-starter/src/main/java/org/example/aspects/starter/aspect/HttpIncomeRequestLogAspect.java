package org.example.aspects.starter.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.example.aspects.starter.annotation.HttpIncomeRequestLog;
import org.example.aspects.starter.config.AspectsProperties;
import org.example.aspects.starter.dto.IncomingHttpLogMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Aspect
public class HttpIncomeRequestLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(HttpIncomeRequestLogAspect.class);

    private final AspectsProperties.HttpRequest properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private String serviceName;

    public HttpIncomeRequestLogAspect(AspectsProperties.HttpRequest properties,
                                      KafkaTemplate<String, Object> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      @Value("${spring.application.name:unknown-service}") String serviceName) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
        System.out.println("=== HTTP REQUEST LOG ASPECT CONSTRUCTOR ===");
        System.out.println("Service: " + serviceName);
        System.out.println("HTTP Request enabled: " + properties.isEnabled());
        System.out.println("Kafka enabled: " + properties.isKafkaEnabled());
    }

    @Pointcut("@annotation(org.example.aspects.starter.annotation.HttpIncomeRequestLog)")
    public void annotatedMethod() {
        logger.debug("=== ANNOTATED METHOD POINTCUT DEFINED ===");

    }

    @Before("annotatedMethod()")
    public void logIncomingRequest(JoinPoint joinPoint) {
        logger.info("=== HTTP REQUEST ASPECT STARTED ===");
        logger.info("Aspect enabled: {}, Kafka enabled: {}",
                properties.isEnabled(), properties.isKafkaEnabled());

        if (!properties.isEnabled()) {
            logger.info("Aspect disabled in configuration - skipping");
            return;
        }

        try {
            logger.info("Processing HTTP request for method: {}",
                    joinPoint.getSignature().toShortString());

            IncomingHttpLogMessage logMessage = createIncomingLogMessage(joinPoint);
            logger.info("Generated log message for URI: {}", logMessage.getUri());

            boolean kafkaSuccess = sendToKafka(logMessage);

            if (!kafkaSuccess && properties.isFallbackToDatabase()) {
                logger.info("Kafka failed - fallback to database");
                handleDatabaseFallback(logMessage, "Kafka unavailable");
            }

            logger.info("=== HTTP REQUEST ASPECT COMPLETED ===");

        } catch (Exception e) {
            logger.error("Failed to log incoming HTTP request: {}", e.getMessage(), e);
        }
    }

    private IncomingHttpLogMessage createIncomingLogMessage(JoinPoint joinPoint) {
        IncomingHttpLogMessage logMessage = new IncomingHttpLogMessage();
        logMessage.setTimestamp(Instant.now());
        logMessage.setServiceName(serviceName);
        logMessage.setMethodSignature(joinPoint.getSignature().toShortString());
        logMessage.setLogLevel("INFO");

        extractHttpRequestInfo(logMessage);

        return logMessage;
    }

    private void extractHttpRequestInfo(IncomingHttpLogMessage logMessage) {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            logMessage.setUri(request.getRequestURI());
            logMessage.setHttpMethod(request.getMethod());
            logMessage.setRemoteAddress(getClientIpAddress(request));

            Map<String, Object> parameters = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                if (values.length == 1) {
                    parameters.put(key, values[0]);
                } else {
                    parameters.put(key, values);
                }
            });
            logMessage.setParameters(parameters);

            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                headers.put(headerName, request.getHeader(headerName));
            }
            logMessage.setHeaders(headers);

            logMessage.setBody("Body available via request wrapper for GET requests");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private boolean sendToKafka(IncomingHttpLogMessage logMessage) {
        if (!properties.isKafkaEnabled()) {
            logger.info("Kafka disabled in configuration for HTTP request logging");
            return false;
        }

        try {
            logger.info("Sending incoming HTTP request log to Kafka topic: {}", properties.getKafkaTopic());

            var message = MessageBuilder
                    .withPayload(logMessage)
                    .setHeader(KafkaHeaders.TOPIC, properties.getKafkaTopic())
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", "INFO")
                    .setHeader("logType", "HTTP_REQUEST")
                    .setHeader("errorTimestamp", Instant.now().toString())
                    .build();

            var result = kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);

            logger.info("Incoming HTTP log sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;

        } catch (Exception e) {
            logger.error("Kafka send failed for incoming HTTP log: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Абстрактный метод для переопределения в микросервисах
     */
    protected void handleDatabaseFallback(IncomingHttpLogMessage logMessage, String kafkaError) {
        logger.warn("DATABASE FALLBACK REQUIRED for HTTP log - Service: {}, URI: {}, Kafka Error: {}",
                logMessage.getServiceName(),
                logMessage.getUri(),
                kafkaError);

        logger.info("Override handleDatabaseFallback method in your microservice to save HTTP logs to database");
    }
}