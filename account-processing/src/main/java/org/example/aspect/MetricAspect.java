package org.example.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.aspect.annotation.Metric;
import org.example.aspect.dto.MetricLogMessage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@RequiredArgsConstructor
public class MetricAspect {

    @Value("${spring.application.name:account-processing}")
    private String serviceName;

    @Value("${metric.execution-time.limit:100}")
    private Long executionTimeLimit;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC_NAME = "service_logs";
    private static final Logger logger = LoggerFactory.getLogger(MetricAspect.class);

    @Pointcut("@annotation(org.example.aspect.annotation.Metric)")
    public void annotatedMethod() {}

    @Around("annotatedMethod() && @annotation(metric)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint, Metric metric) throws Throwable {
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
            if (executionTime > executionTimeLimit) {
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
                metricName, methodSignature, executionTime, executionTimeLimit);

        if (executionTime > executionTimeLimit) {
            logger.warn("EXCEEDED LIMIT - Method: {} took {} ms (limit: {} ms)",
                    methodSignature, executionTime, executionTimeLimit);
        }
    }

    private void handleSlowExecution(ProceedingJoinPoint joinPoint, String methodSignature,
                                     long executionTime, String metricName) {
        try {
            logger.warn("SLOW EXECUTION DETECTED - Method: {}, Time: {} ms, Limit: {} ms",
                    methodSignature, executionTime, executionTimeLimit);

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
        message.setMethodSignature(methodSignature);
        message.setExecutionTimeMs(executionTime);
        message.setInputParameters(getInputParameters(joinPoint));
        message.setMetricName(metricName);
        message.setWarningReason(String.format("Execution time %d ms exceeds limit %d ms",
                executionTime, executionTimeLimit));
        return message;
    }

    private void sendMetricWarningToKafka(MetricLogMessage metricMessage) {
        try {
            logger.info("Sending metric warning to Kafka topic: {}", TOPIC_NAME);

            var message = MessageBuilder
                    .withPayload(metricMessage)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC_NAME)
                    .setHeader(KafkaHeaders.KEY, serviceName)
                    .setHeader("type", "WARNING")
                    .setHeader("metricType", "SLOW_EXECUTION")
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
}