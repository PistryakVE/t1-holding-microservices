CREATE TABLE blacklist_registry (
    id BIGSERIAL PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL,
    reason TEXT,
    blacklist_expiration_date TIMESTAMP
);
INSERT INTO blacklist_registry (document_type, document_id, blacklisted_at, reason, blacklist_expiration_date) VALUES
('PASSPORT', '1234567890', '2024-01-15 10:00:00', 'Подозрение в мошенничестве', '2025-01-15 10:00:00'),
('PASSPORT', '0987654321', '2024-02-20 14:30:00', 'Невыполнение обязательств', NULL),
('PASSPORT', '1122334455', '2024-03-10 09:15:00', 'Нарушение правил', '2024-09-10 09:15:00'),
('PASSPORT', '5566778899', '2024-01-05 16:45:00', 'Просроченные платежи', NULL),
('PASSPORT', '9999888877', '2024-04-01 11:20:00', 'Проблемы с верификацией', '2024-12-01 11:20:00');