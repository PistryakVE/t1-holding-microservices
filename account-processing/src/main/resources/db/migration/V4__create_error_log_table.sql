CREATE TABLE error_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    method_signature TEXT NOT NULL,
    stack_trace TEXT,
    error_message TEXT,
    input_parameters TEXT,
    kafka_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);