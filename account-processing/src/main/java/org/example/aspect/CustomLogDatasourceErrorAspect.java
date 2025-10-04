package org.example.aspect;

import jakarta.annotation.PostConstruct;
import org.aspectj.lang.annotation.Aspect;
import org.example.aspects.starter.annotation.LogDatasourceError;
import org.example.aspects.starter.aspect.LogDatasourceErrorAspect;
import org.example.aspects.starter.config.AspectsProperties;
import org.example.aspects.starter.dto.ErrorLogMessage;
import org.example.aspect.entity.ErrorLogEntity;
import org.example.repository.ErrorLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Aspect
@Component
public class CustomLogDatasourceErrorAspect extends LogDatasourceErrorAspect {

    private final ErrorLogRepository errorLogRepository;
    private final ObjectMapper objectMapper;

    public CustomLogDatasourceErrorAspect(AspectsProperties aspectsProperties,
                                          KafkaTemplate<String, Object> kafkaTemplate,
                                          ObjectMapper objectMapper,
                                          ErrorLogRepository errorLogRepository,
                                          @Value("${spring.application.name:unknown-service}") String serviceName) {  // ДОБАВИТЬ serviceName
        // Передаем serviceName в родительский конструктор
        super(aspectsProperties.getDatasource(), kafkaTemplate, objectMapper, serviceName);
        this.errorLogRepository = errorLogRepository;
        this.objectMapper = objectMapper;
    }
    @PostConstruct
    public void init() {
        log.info("=== CUSTOM LOG DATASOURCE ERROR ASPECT INITIALIZED ===");
    }
    @Override
    protected void handleDatabaseFallback(ErrorLogMessage errorMessage, LogDatasourceError.LogLevel level, String kafkaError) {
        try {
            log.info("Saving error log to database due to Kafka unavailability");

            ErrorLogEntity errorLog = new ErrorLogEntity();
            errorLog.setTimestamp(errorMessage.getTimestamp());
            errorLog.setServiceName(errorMessage.getServiceName());
            errorLog.setLevel(errorMessage.getErrorLevel()); // используем errorLevel из DTO
            errorLog.setMethodSignature(errorMessage.getMethodSignature());
            errorLog.setStackTrace(errorMessage.getStackTrace());
            errorLog.setErrorMessage(errorMessage.getErrorMessage());
            errorLog.setKafkaError(kafkaError);

            // Сериализуем параметры в JSON строку
            errorLog.setInputParameters(serializeInputParameters(errorMessage.getInputParameters()));

            errorLogRepository.save(errorLog);
            log.info("Error log successfully saved to database with ID: {}", errorLog.getId());

        } catch (Exception e) {
            log.error("Failed to save error log to database: {}", e.getMessage());
            // Логируем в консоль как последнее средство
            log.error("FALLBACK ERROR - Service: {}, Method: {}, Error: {}",
                    errorMessage.getServiceName(),
                    errorMessage.getMethodSignature(),
                    errorMessage.getErrorMessage());
        }
    }

    private String serializeInputParameters(List<Object> inputParameters) {
        if (inputParameters == null || inputParameters.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(inputParameters);
        } catch (Exception e) {
            log.warn("Failed to serialize input parameters to JSON, using toString: {}", e.getMessage());
            // Fallback: преобразуем в строку
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < inputParameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(inputParameters.get(i) != null ? inputParameters.get(i).toString() : "null");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}