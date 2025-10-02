package org.example.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.aspect.annotation.LogDatasourceError;
import org.example.aspect.dto.ErrorLogMessage;
import org.example.aspect.entity.ErrorLogEntity;
import org.example.repository.ErrorLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

@Aspect
@Component
@RequiredArgsConstructor
public class LogDatasourceErrorAspect {

    @Value("${spring.application.name:account-processing}")
    private String serviceName;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ErrorLogRepository errorLogRepository;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_NAME = "service_logs";
    private static final Logger logger = LoggerFactory.getLogger(LogDatasourceErrorAspect.class);

    @PostConstruct
    public void init() {
        logger.info("LOG DATASOURCE ERROR ASPECT INITIALIZED ===");
        logger.info("Service name: {}", serviceName);
    }

    @AfterThrowing(
            pointcut = "@annotation(logDatasourceError)",
            throwing = "ex"
    )
    public void logDataSourceError(JoinPoint joinPoint, LogDatasourceError logDatasourceError, Throwable ex) {
        try {
            logger.info("DATASOURCE ERROR ASPECT TRIGGERED ===");
            logger.info("Method: {}, Level: {}", joinPoint.getSignature().toShortString(), logDatasourceError.level());

            // Создаем сообщение для Kafka
            ErrorLogMessage errorMessage = createErrorMessage(joinPoint, ex, logDatasourceError.level());

            // Пытаемся отправить в Kafka
            boolean kafkaSuccess = sendToKafka(errorMessage, logDatasourceError.level());

            if (!kafkaSuccess) {
                // Если Kafka недоступна, пишем в БД
                logToDatabase(errorMessage, logDatasourceError.level(), "Kafka unavailable");
            }

            // Всегда логируем в консоль
            logToConsole(joinPoint, ex, logDatasourceError.level());

        } catch (Exception aspectEx) {
            // Если даже аспект упал, логируем минимальную информацию
            logger.error("CRITICAL: LogDatasourceError aspect failed: {}", aspectEx.getMessage());
            logger.error("Original error: {}", ex.getMessage(), ex);
        }
    }

    private ErrorLogMessage createErrorMessage(JoinPoint joinPoint, Throwable ex, LogDatasourceError.LogLevel level) {
        ErrorLogMessage errorMessage = new ErrorLogMessage();
        errorMessage.setTimestamp(Instant.now());
        errorMessage.setMethodSignature(joinPoint.getSignature().toShortString());
        errorMessage.setStackTrace(getStackTrace(ex));
        errorMessage.setErrorMessage(ex.getMessage());
        errorMessage.setInputParameters(getInputParameters(joinPoint));
        return errorMessage;
    }

    private boolean sendToKafka(ErrorLogMessage errorMessage, LogDatasourceError.LogLevel level) {
        try {
            logger.info("Sending datasource error log to Kafka topic: {}", TOPIC_NAME);

            var message = MessageBuilder
                    .withPayload(errorMessage)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", level.name())
                    .setHeader("errorType", "DATASOURCE_ERROR")
                    .setHeader("timestamp", Instant.now().toString())
                    .build();

            CompletableFuture<Boolean> future = new CompletableFuture<>();

            kafkaTemplate.send(message)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logger.error("Kafka send failed for datasource error: {}", exception.getMessage());
                            future.complete(false);
                        } else {
                            logger.info("Datasource error log sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            future.complete(true);
                        }
                    });

            return future.get();

        } catch (Exception e) {
            logger.error("Error preparing Kafka message for datasource error: {}", e.getMessage());
            return false;
        }
    }

    private void logToDatabase(ErrorLogMessage errorMessage, LogDatasourceError.LogLevel level, String kafkaError) {
        try {
            ErrorLogEntity errorLog = new ErrorLogEntity();
            errorLog.setTimestamp(errorMessage.getTimestamp());
            errorLog.setServiceName(serviceName);
            errorLog.setLevel(level.name());
            errorLog.setMethodSignature(errorMessage.getMethodSignature());
            errorLog.setStackTrace(errorMessage.getStackTrace());
            errorLog.setErrorMessage(errorMessage.getErrorMessage());
            errorLog.setInputParameters(convertParametersToJson(errorMessage.getInputParameters()));
            errorLog.setKafkaError(kafkaError);

            ErrorLogEntity savedLog = errorLogRepository.save(errorLog);
            logger.info("Error log saved to database with ID: {}", savedLog.getId());

        } catch (Exception dbEx) {
            logger.error("CRITICAL: Failed to write error log to database: {}", dbEx.getMessage());
        }
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
            // Пытаемся сериализовать объект безопасно
            if (obj == null) {
                return "null";
            }

            // Для простых типов возвращаем как есть
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
                return obj;
            }

            // Для сложных объектов пытаемся конвертировать
            return objectMapper.convertValue(obj, Object.class);

        } catch (Exception e) {
            // Если не удалось сериализовать, возвращаем строковое представление
            return obj != null ? obj.toString() : "null";
        }
    }

    private String convertParametersToJson(List<Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            return "[\"Failed to serialize parameters\"]";
        }
    }
}