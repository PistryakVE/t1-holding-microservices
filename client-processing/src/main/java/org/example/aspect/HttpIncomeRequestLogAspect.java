//package org.example.aspect;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.example.aspect.annotation.HttpIncomeRequestLog;
//import org.example.aspect.dto.IncomingHttpLogMessage;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.aspectj.lang.annotation.Pointcut;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.kafka.support.SendResult;
//import org.springframework.messaging.support.MessageBuilder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import java.time.Instant;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Aspect
//@Component
//@RequiredArgsConstructor
//public class HttpIncomeRequestLogAspect {
//
//    @Value("${spring.application.name:client-processing}")
//    private String serviceName;
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//
//    private static final String TOPIC_NAME = "service_logs";
//    private static final Logger logger = LoggerFactory.getLogger(HttpIncomeRequestLogAspect.class);
//
//    @Pointcut("@annotation(org.example.aspect.annotation.HttpIncomeRequestLog)")
//    public void annotatedMethod() {}
//
//    @Before("annotatedMethod()")
//    public void logIncomingRequest(JoinPoint joinPoint) {
//        try {
//            logger.info("INCOMING HTTP REQUEST ASPECT TRIGGERED ===");
//
//            IncomingHttpLogMessage logMessage = createIncomingLogMessage(joinPoint);
//            logger.info("Generated incoming log message for URI: {}", logMessage.getUri());
//
//            sendToKafka(logMessage);
//
//        } catch (Exception e) {
//            logger.error("Failed to log incoming HTTP request: {}", e.getMessage());
//        }
//    }
//
//    private IncomingHttpLogMessage createIncomingLogMessage(JoinPoint joinPoint) {
//        IncomingHttpLogMessage logMessage = new IncomingHttpLogMessage();
//        logMessage.setTimestamp(Instant.now());
//        logMessage.setMethodSignature(joinPoint.getSignature().toShortString());
//
//        extractHttpRequestInfo(logMessage);
//
//        return logMessage;
//    }
//
//    private void extractHttpRequestInfo(IncomingHttpLogMessage logMessage) {
//        ServletRequestAttributes attributes = (ServletRequestAttributes)
//                RequestContextHolder.getRequestAttributes();
//
//        if (attributes != null) {
//            HttpServletRequest request = attributes.getRequest();
//
//            logMessage.setUri(request.getRequestURI());
//            logMessage.setHttpMethod(request.getMethod());
//            logMessage.setRemoteAddress(getClientIpAddress(request));
//
//            Map<String, Object> parameters = new HashMap<>();
//            request.getParameterMap().forEach((key, values) -> {
//                if (values.length == 1) {
//                    parameters.put(key, values[0]);
//                } else {
//                    parameters.put(key, values);
//                }
//            });
//            logMessage.setParameters(parameters);
//
//            Map<String, String> headers = new HashMap<>();
//            Enumeration<String> headerNames = request.getHeaderNames();
//            while (headerNames.hasMoreElements()) {
//                String headerName = headerNames.nextElement();
//                headers.put(headerName, request.getHeader(headerName));
//            }
//            logMessage.setHeaders(headers);
//
//            logMessage.setBody("Body available via request wrapper for GET requests");
//        }
//    }
//
//    private String getClientIpAddress(HttpServletRequest request) {
//        String xForwardedFor = request.getHeader("X-Forwarded-For");
//        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
//            return xForwardedFor.split(",")[0].trim();
//        }
//
//        String xRealIp = request.getHeader("X-Real-IP");
//        if (xRealIp != null && !xRealIp.isEmpty()) {
//            return xRealIp;
//        }
//
//        return request.getRemoteAddr();
//    }
//
//    private CompletableFuture<SendResult<String, Object>> sendToKafka(IncomingHttpLogMessage logMessage) {
//        try {
//            logger.info("Sending incoming HTTP request log to Kafka topic: {}", TOPIC_NAME);
//
//            // Создаем сообщение с заголовками
//            var message = MessageBuilder
//                    .withPayload(logMessage)
//                    .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
//                    .setHeader(KafkaHeaders.KEY, serviceName)
//                    .setHeader("type", "INFO")
//                    .build();
//
//            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);
//
//            future.whenComplete((result, exception) -> {
//                if (exception != null) {
//                    logger.error("Kafka send failed for incoming HTTP log: {}", exception.getMessage());
//                } else {
//                    logger.info("Incoming HTTP log sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
//                            result.getRecordMetadata().topic(),
//                            result.getRecordMetadata().partition(),
//                            result.getRecordMetadata().offset());
//                }
//            });
//
//            return future;
//
//        } catch (Exception e) {
//            logger.error("Error preparing Kafka message for incoming HTTP log: {}", e.getMessage());
//            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
//            failedFuture.completeExceptionally(e);
//            return failedFuture;
//        }
//    }
//}