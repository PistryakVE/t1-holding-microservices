package org.example.aspects.starter.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.aspects.starter.annotation.LogDatasourceError;
import org.example.aspects.starter.config.AspectsProperties;
import org.example.aspects.starter.dto.ErrorLogMessage;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Aspect
public class LogDatasourceErrorAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogDatasourceErrorAspect.class);

    private final AspectsProperties.Datasource properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private String serviceName;

    // В LogDatasourceErrorAspect (стартер)
    public LogDatasourceErrorAspect(AspectsProperties.Datasource properties,
                                    KafkaTemplate<String, Object> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${spring.application.name:unknown-service}") String serviceName) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }
    @PostConstruct
    public void init() {
        logger.info("=== BASE LOG DATASOURCE ERROR ASPECT INITIALIZED ===");
        logger.info("Service name: {}", serviceName);
        logger.info("Properties enabled: {}", properties.isEnabled());
    }

    @AfterThrowing(
            pointcut = "@annotation(logDatasourceError)",
            throwing = "ex"
    )
    public void logDataSourceError(JoinPoint joinPoint, LogDatasourceError logDatasourceError, Throwable ex) {

        // ДОБАВИТЬ: проверка на дублирование вызовов
        String methodSignature = joinPoint.getSignature().toShortString();
        String exceptionMsg = ex.getMessage();
        String uniqueKey = methodSignature + ":" + exceptionMsg + ":" + Instant.now().getEpochSecond();

        logger.info("=== ASPECT TRIGGERED ===");
        logger.info("Method: {}, Exception: {}", methodSignature, exceptionMsg);
        logger.info("Aspect enabled: {}, Kafka enabled: {}, Fallback enabled: {}",
                properties.isEnabled(),
                properties.isKafkaEnabled(),
                properties.isFallbackToDatabase());

        if (!properties.isEnabled()) {
            logger.info("Aspect disabled in configuration - skipping");
            return;
        }

        try {
            // ДОБАВИТЬ: проверка - не логируем исключения самого аспекта
            if (ex.getClass().getName().contains("LogDatasourceErrorAspect") ||
                    ex.getClass().getName().contains("Kafka")) {
                logger.debug("Skipping aspect for Kafka or aspect-related exceptions");
                return;
            }

            // Создаем сообщение для Kafka
            ErrorLogMessage errorMessage = createErrorMessage(joinPoint, ex, logDatasourceError.level());

            // Пытаемся отправить в Kafka
            boolean kafkaSuccess = sendToKafka(errorMessage, logDatasourceError.level());

            logger.info("Kafka send result: {}, Fallback enabled: {}",
                    kafkaSuccess, properties.isFallbackToDatabase());

            if (!kafkaSuccess && properties.isFallbackToDatabase()) {
                logger.info("Kafka failed and fallback enabled - writing to database");
                String kafkaError = "Kafka unavailable or send failed";
                handleDatabaseFallback(errorMessage, logDatasourceError.level(), kafkaError);
            } else if (!kafkaSuccess) {
                logger.info("Kafka failed but fallback disabled - skipping database write");
            }

            // Всегда логируем в консоль
            logToConsole(joinPoint, ex, logDatasourceError.level());

        } catch (Exception aspectEx) {
            logger.error("CRITICAL: LogDatasourceError aspect failed: {}", aspectEx.getMessage());
            logger.error("Original error: {}", ex.getMessage(), ex);
        }
    }

    private boolean sendToKafka(ErrorLogMessage errorMessage, LogDatasourceError.LogLevel level) {
        logger.info("=== KAFKA SEND ATTEMPT ===");
        logger.info("Kafka enabled: {}, Topic: {}, Fallback to DB: {}",
                properties.isKafkaEnabled(),
                properties.getKafkaTopic(),
                properties.isFallbackToDatabase());

        if (!properties.isKafkaEnabled()) {
            logger.info("Kafka disabled in configuration - skipping Kafka send");
            return false;
        }

        try {
            logger.info("Attempting to send message to Kafka topic: {}", properties.getKafkaTopic());
            logger.info("Message payload: {}", objectMapper.writeValueAsString(errorMessage));

            var message = MessageBuilder
                    .withPayload(errorMessage)
                    .setHeader(KafkaHeaders.TOPIC, properties.getKafkaTopic())
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", level.name())
                    .setHeader("errorType", "DATASOURCE_ERROR")
                    // УДАЛИТЬ этот заголовок - он зарезервирован!
                    // .setHeader("timestamp", Instant.now().toString())
                    .setHeader("errorTimestamp", Instant.now().toString()) // Использовать другое имя
                    .build();

            // Синхронная отправка для надежности
            var result = kafkaTemplate.send(message).get(5, TimeUnit.SECONDS);

            logger.info("Datasource error log sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;

        } catch (Exception e) {
            logger.error("Kafka send failed for datasource error: {}", e.getMessage());
            logger.error("Kafka exception details:", e);
            return false;
        }
    }

    /**
     * АБСТРАКТНЫЙ метод для переопределения в микросервисах
     * Микросервисы должны предоставить свою реализацию через наследование
     */
    protected void handleDatabaseFallback(ErrorLogMessage errorMessage, LogDatasourceError.LogLevel level, String kafkaError) {
        // Базовая реализация - просто логируем
        logger.warn("DATABASE FALLBACK REQUIRED - Service: {}, Method: {}, Error: {}, Kafka Error: {}",
                errorMessage.getServiceName(),
                errorMessage.getMethodSignature(),
                errorMessage.getErrorMessage(),
                kafkaError);

        logger.info("Override handleDatabaseFallback method in your microservice to save to database");
        logger.info("Example implementation: save ErrorLogMessage to error_log table");
    }

    // остальные методы без изменений...
    private ErrorLogMessage createErrorMessage(JoinPoint joinPoint, Throwable ex, LogDatasourceError.LogLevel level) {
        ErrorLogMessage errorMessage = new ErrorLogMessage();
        errorMessage.setTimestamp(Instant.now());
        errorMessage.setServiceName(serviceName);
        errorMessage.setMethodSignature(joinPoint.getSignature().toShortString());
        errorMessage.setStackTrace(getStackTrace(ex));
        errorMessage.setErrorMessage(ex.getMessage());
        errorMessage.setErrorLevel(level.name());
        errorMessage.setInputParameters(getInputParameters(joinPoint));
        errorMessage.setExceptionType(ex.getClass().getSimpleName());
        return errorMessage;
    }

    private void logToConsole(JoinPoint joinPoint, Throwable ex, LogDatasourceError.LogLevel level) {
        String logMessage = String.format("DATASOURCE ERROR - Service: %s, Method: %s, Error: %s",
                serviceName, joinPoint.getSignature().toShortString(), ex.getMessage());

        switch (level) {
            case ERROR -> logger.error(logMessage, ex);
            case WARNING -> logger.warn(logMessage, ex);
            case INFO -> logger.info(logMessage, ex);
        }
    }

    private String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

    private List<Object> getInputParameters(JoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .map(this::safeSerialize)
                .toList();
    }

    private Object safeSerialize(Object obj) {
        try {
            if (obj == null) return "null";
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) return obj;
            return objectMapper.convertValue(obj, Object.class);
        } catch (Exception e) {
            return obj != null ? obj.toString() : "null";
        }
    }

}