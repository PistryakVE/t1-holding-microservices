package org.example.aspect.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "error_log")
@Data
public class ErrorLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String level;

    @Column(name = "method_signature", nullable = false)
    private String methodSignature;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "input_parameters", columnDefinition = "TEXT")
    private String inputParameters;

    @Column(name = "kafka_error", columnDefinition = "TEXT")
    private String kafkaError;
}