package org.example.aspects.starter.config;

import org.example.aspects.starter.aspect.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(AspectsProperties.class)
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
public class AspectsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "aspects.metric", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
    public MetricAspect metricAspect(AspectsProperties properties,
                                     KafkaTemplate<String, Object> kafkaTemplate,
                                     ObjectMapper objectMapper) {
        return new MetricAspect(properties.getMetric(), kafkaTemplate, objectMapper);
    }

    @Bean
    public HttpIncomeRequestLogAspect httpIncomeRequestLogAspect(
            AspectsProperties aspectsProperties,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        System.out.println("=== CREATING HTTP INCOME REQUEST LOG ASPECT ===");
        System.out.println("Service: " + serviceName);
        System.out.println("HTTP Request enabled: " + aspectsProperties.getHttpRequest().isEnabled());
        return new HttpIncomeRequestLogAspect(
                aspectsProperties.getHttpRequest(),
                kafkaTemplate,
                objectMapper,
                serviceName
        );
    }

    @Bean
    public HttpOutcomeRequestLogAspect httpOutcomeRequestLogAspect(
            AspectsProperties aspectsProperties,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:unknown-service}") String serviceName) {
        return new HttpOutcomeRequestLogAspect(
                aspectsProperties.getHttpRequest(),
                kafkaTemplate,
                objectMapper,
                serviceName
        );
    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnProperty(prefix = "aspects.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
//    public CachedAspect cachedAspect(AspectsProperties properties) {
//        return new CachedAspect(properties.getCache());
//    }

//    @Bean
//    @ConditionalOnProperty(name = "aspects.datasource.enabled", havingValue = "true")
//    public LogDatasourceErrorAspect logDatasourceErrorAspect(
//            AspectsProperties.Datasource properties,
//            KafkaTemplate<String, Object> kafkaTemplate,
//            ObjectMapper objectMapper) {
//        return new LogDatasourceErrorAspect(properties, kafkaTemplate, objectMapper);
//    }
}