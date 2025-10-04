package org.example.aspects.starter.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.aspects.starter.annotation.Metric;
import org.example.aspects.starter.config.AspectsProperties;
import org.example.aspects.starter.dto.MetricLogMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Aspect
public class MetricAspect {

    private static final Logger logger = LoggerFactory.getLogger(MetricAspect.class);

    private final AspectsProperties.Metric properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private String serviceName;

    // Конструктор для Autoconfiguration
    public MetricAspect(AspectsProperties.Metric properties,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@annotation(org.example.aspects.starter.annotation.Metric)")
    public void annotatedMethod() {}

    @Around("annotatedMethod() && @annotation(metric)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Metric metric) throws Throwable {
        // Проверяем, включен ли аспект
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        String methodSignature = joinPoint.getSignature().toShortString();
        String metricName = metric.value().isEmpty() ? methodSignature : metric.value();

        try {
            logger.debug("Starting metric measurement for method: {}", methodSignature);

            // Выполняем оригинальный метод
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Логируем время выполнения
            logExecutionTime(methodSignature, executionTime, metricName);

            // Проверяем превышение лимита
            if (executionTime > properties.getExecutionTimeLimit()) {
                handleSlowExecution(joinPoint, methodSignature, executionTime, metricName);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.warn("Method {} failed after {} ms", methodSignature, executionTime);
            throw throwable;
        }
    }

    private void logExecutionTime(String methodSignature, long executionTime, String metricName) {
        // Всегда логируем для отладки
        logger.info("METRIC [{}] - Method: {}, Execution time: {} ms, Limit: {} ms",
                metricName, methodSignature, executionTime, properties.getExecutionTimeLimit());

        if (executionTime > properties.getExecutionTimeLimit()) {
            logger.warn("EXCEEDED LIMIT - Method: {} took {} ms (limit: {} ms)",
                    methodSignature, executionTime, properties.getExecutionTimeLimit());
        }
    }

    private void handleSlowExecution(ProceedingJoinPoint joinPoint, String methodSignature,
                                     long executionTime, String metricName) {
        try {
            logger.warn("SLOW EXECUTION DETECTED - Method: {}, Time: {} ms, Limit: {} ms",
                    methodSignature, executionTime, properties.getExecutionTimeLimit());

            MetricLogMessage metricMessage = createMetricMessage(joinPoint, methodSignature, executionTime, metricName);
            sendMetricWarningToKafka(metricMessage);

        } catch (Exception e) {
            logger.error("Failed to process slow execution metric for method: {}", methodSignature, e);
        }
    }

    private MetricLogMessage createMetricMessage(ProceedingJoinPoint joinPoint, String methodSignature,
                                                 long executionTime, String metricName) {
        MetricLogMessage message = new MetricLogMessage();
        message.setTimestamp(Instant.now());
        message.setServiceName(serviceName);
        message.setMethodSignature(methodSignature);
        message.setExecutionTimeMs(executionTime);
        message.setInputParameters(getInputParameters(joinPoint));
        message.setMetricName(metricName);
        message.setWarningReason(String.format("Execution time %d ms exceeds limit %d ms",
                executionTime, properties.getExecutionTimeLimit()));
        return message;
    }

    private void sendMetricWarningToKafka(MetricLogMessage metricMessage) {
        if (!properties.isKafkaEnabled()) {
            logger.debug("Kafka disabled in config, skipping metric warning send");
            return;
        }

        try {
            logger.info("Sending metric warning to Kafka topic: {}", properties.getKafkaTopic());

            var message = MessageBuilder
                    .withPayload(metricMessage)
                    .setHeader(KafkaHeaders.TOPIC, properties.getKafkaTopic())
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", "WARNING")
                    .setHeader("metricType", "SLOW_EXECUTION")
                    .setHeader("timestamp", Instant.now().toString())
                    .build();

            kafkaTemplate.send(message)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            logger.error("Kafka send failed for metric warning: {}", exception.getMessage());
                        } else {
                            logger.info("Metric warning sent successfully to Kafka. Topic: {}, Partition: {}, Offset: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            logger.error("Error preparing Kafka message for metric warning: {}", e.getMessage());
        }
    }

    private List<Object> getInputParameters(ProceedingJoinPoint joinPoint) {
        return Arrays.stream(joinPoint.getArgs())
                .map(this::safeSerialize)
                .toList();
    }

    private Object safeSerialize(Object obj) {
        try {
            if (obj == null) {
                return "null";
            }
            if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
                return obj;
            }
            return objectMapper.convertValue(obj, Object.class);
        } catch (Exception e) {
            return obj != null ? obj.toString() : "null";
        }
    }

    @Autowired
    public void setServiceName(@org.springframework.beans.factory.annotation.Value("${spring.application.name:unknown-service}") String serviceName) {
        this.serviceName = serviceName;
    }
}