ALTER TABLE payments
ADD COLUMN monthly_payment DECIMAL(15,2),
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
ADD COLUMN expired BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE payments
SET
    monthly_payment = CASE
        WHEN is_credit = TRUE THEN amount / 12  -- Для кредитных рассрочиваем на 12 месяцев
        ELSE NULL
    END,
    status = CASE
        WHEN payed_at IS NOT NULL THEN 'COMPLETED'
        ELSE 'PENDING'
    END,
    expired = CASE
        WHEN is_credit = TRUE AND payed_at IS NULL AND payment_date < NOW() THEN TRUE
        ELSE FALSE
    END;
INSERT INTO payments (account_id, payment_date, amount, monthly_payment, is_credit, payed_at, type, status, expired) VALUES
-- Кредитные платежи (с графиком)
(1, '2024-04-15 10:30:00', 12000.00, 1000.00, TRUE, NULL, 'LOAN_PAYMENT', 'PENDING', FALSE),

-- Просроченные кредитные платежи
(2, '2024-02-10 09:00:00', 5000.00, 416.67, TRUE, NULL, 'LOAN_PAYMENT', 'PENDING', TRUE),

-- Завершенные кредитные платежи
(3, '2024-02-05 14:20:00', 8000.00, 666.67, TRUE, '2024-02-05 14:22:00', 'LOAN_PAYMENT', 'COMPLETED', FALSE),

-- Текущие дебетовые операции (без monthly_payment)
(4, '2024-02-01 12:00:00', 1500.00, NULL, FALSE, '2024-02-01 12:05:00', 'WITHDRAWAL', 'COMPLETED', FALSE),

-- Кредитные карточные платежи с разными статусами
(5, '2024-01-20 11:30:00', 3000.00, 250.00, TRUE, NULL, 'CREDIT_CARD_PAYMENT', 'PENDING', TRUE),

-- Депозиты и переводы
(2, '2024-02-27 10:00:00', 2500.00, NULL, FALSE, '2024-02-27 10:01:00', 'TRANSFER', 'COMPLETED', FALSE);