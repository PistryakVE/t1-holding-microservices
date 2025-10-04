//package org.example.aspect;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.example.aspect.dto.OutgoingHttpLogMessage;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.AfterReturning;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.time.Instant;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Aspect
//@Component
//@RequiredArgsConstructor
//public class HttpOutcomeRequestLogAspect {
//
//    @Value("${spring.application.name:credit-processing}")
//    private String serviceName;
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//
//    private static final String TOPIC_NAME = "service_logs";
//    private static final Logger logger = LoggerFactory.getLogger(HttpOutcomeRequestLogAspect.class);
//
//    @PostConstruct
//    public void init() {
//        logger.info("HTTP OUTCOME ASPECT INITIALIZED ===");
//        logger.info("Service name: {}", serviceName);
//        logger.info("Kafka template: {}", kafkaTemplate != null ? "AVAILABLE" : "NULL");
//        logger.info("ObjectMapper: {}", objectMapper != null ? "AVAILABLE" : "NULL");
//    }
//
//    @Pointcut("@annotation(org.example.aspect.annotation.HttpOutcomeRequestLog)")
//    public void annotatedMethod() {}
//
//    @Pointcut("execution(* org.example.service.ProductRegistryService.*(..))")
//    public void productRegistryServiceMethods() {}
//
//    @Pointcut("execution(* org.example.service.ProductRegistryService.makeHttpCallWithLogging(..))")
//    public void makeHttpCallMethod() {}
//
//    @AfterReturning(pointcut = "annotatedMethod()", returning = "result")
//    public void logAnnotatedHttpRequest(JoinPoint joinPoint, Object result) {
//        logger.info("ANNOTATED METHOD ASPECT TRIGGERED ===");
//        logger.info("Method: {}", joinPoint.getSignature().toShortString());
//
//        try {
//            String message = createOutgoingLogMessage(joinPoint, result);
//            logger.info("Generated log message: {}", message);
//            sendToKafka(message);
//        } catch (Exception e) {
//            logger.error("Failed to log annotated HTTP request: {}", e.getMessage());
//        }
//    }
//
//    private String createOutgoingLogMessage(JoinPoint joinPoint, Object result) throws Exception {
//        OutgoingHttpLogMessage logMessage = new OutgoingHttpLogMessage();
//        logMessage.setTimestamp(Instant.now());
//        logMessage.setMethodSignature(joinPoint.getSignature().toShortString());
//
//        Object[] args = joinPoint.getArgs();
//        extractHttpInfo(args, logMessage);
//
//        if (result instanceof ResponseEntity) {
//            ResponseEntity<?> response = (ResponseEntity<?>) result;
//            Map<String, Object> responseInfo = new HashMap<>();
//            responseInfo.put("statusCode", response.getStatusCodeValue());
//            responseInfo.put("statusText", response.getStatusCode().toString());
//            responseInfo.put("body", response.getBody());
//            logMessage.setBody(responseInfo);
//        }
//
//        return objectMapper.writeValueAsString(logMessage);
//    }
//
//    private void extractHttpInfo(Object[] args, OutgoingHttpLogMessage logMessage) {
//        for (Object arg : args) {
//            if (arg instanceof String && ((String) arg).startsWith("http")) {
//                logMessage.setUri((String) arg);
//                logMessage.setHttpMethod("GET");
//            } else if (arg instanceof URI) {
//                logMessage.setUri(arg.toString());
//                logMessage.setHttpMethod("GET");
//            } else if (arg instanceof HttpEntity) {
//                HttpEntity<?> entity = (HttpEntity<?>) arg;
//                logMessage.setBody(entity.getBody());
//
//                if (entity.getHeaders() != null) {
//                    Map<String, String> headers = new HashMap<>();
//                    entity.getHeaders().forEach((key, values) -> {
//                        if (values != null && !values.isEmpty()) {
//                            headers.put(key, String.join(", ", values));
//                        }
//                    });
//                    logMessage.setHeaders(headers);
//                }
//            } else if (arg instanceof HttpMethod) {
//                logMessage.setHttpMethod(((HttpMethod) arg).name());
//            }
//        }
//
//        if (logMessage.getUri() == null) {
//            for (Object arg : args) {
//                if (arg != null && arg.toString().contains("http")) {
//                    logMessage.setUri(arg.toString());
//                    break;
//                }
//            }
//        }
//    }
//
//    private CompletableFuture<SendResult<String, Object>> sendToKafka(String message) {
//        try {
//            logger.info("Sending HTTP request log to Kafka topic: {}", TOPIC_NAME);
//
//            CompletableFuture<SendResult<String, Object>> future =
//                    kafkaTemplate.send(TOPIC_NAME, serviceName, message);
//
//            future.whenComplete((result, exception) -> {
//                if (exception != null) {
//                    logger.error("Kafka send failed for HTTP log: {}", exception.getMessage());
//                } else {
//                    logger.info("HTTP log sent successfully to Kafka: {}", result.getRecordMetadata());
//                }
//            });
//
//            return future;
//
//        } catch (Exception e) {
//            logger.error("Error preparing Kafka message for HTTP log: {}", e.getMessage());
//            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//}