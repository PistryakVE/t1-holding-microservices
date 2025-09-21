-- Вставка тестовых данных в accounts
INSERT INTO accounts (client_id, product_id, balance, interest_rate, is_recalc, card_exist, status) VALUES
('770100000001', 'DC1', 150000.00, 1.50, TRUE, TRUE, 'ACTIVE'),
('770100000001', 'CC1', -25000.00, 15.99, FALSE, TRUE, 'ACTIVE'),
('770100000002', 'DC1', 75000.00, 1.20, TRUE, TRUE, 'ACTIVE'),
('770200000001', 'DC1', 230000.00, 1.80, TRUE, TRUE, 'ACTIVE'),
('770200000002', 'AC1', 500000.00, 5.50, FALSE, FALSE, 'ACTIVE');

-- Вставка тестовых данных в cards
INSERT INTO cards (account_id, card_id, payment_system, status) VALUES
(1, '1234567890123456', 'VISA', 'ACTIVE'),
(1, '1234567890123457', 'VISA', 'ACTIVE'),
(2, '9876543210987654', 'MASTERCARD', 'ACTIVE'),
(3, '5678901234567890', 'MIR', 'ACTIVE'),
(4, '4321098765432109', 'VISA', 'ACTIVE');

-- Вставка тестовых данных в payments
INSERT INTO payments (account_id, payment_date, amount, is_credit, payed_at, type) VALUES
(1, '2024-01-15 10:30:00', 5000.00, TRUE, '2024-01-15 10:30:00', 'DEPOSIT'),
(1, '2024-01-20 14:15:00', 2500.00, FALSE, '2024-01-20 14:15:00', 'WITHDRAWAL'),
(2, '2024-01-18 09:45:00', 10000.00, TRUE, '2024-01-18 09:45:00', 'TRANSFER'),
(3, '2024-01-22 16:20:00', 7500.00, TRUE, '2024-01-22 16:20:00', 'DEPOSIT'),
(4, '2024-01-25 11:30:00', 30000.00, TRUE, '2024-01-25 11:30:00', 'TRANSFER');

-- Вставка тестовых данных в transactions
INSERT INTO transactions (account_id, card_id, type, amount, status, timestamp) VALUES
(1, 1, 'DEBIT', 5000.00, 'COMPLETE', '2024-01-15 10:30:00'),
(1, 1, 'CREDIT', 2500.00, 'COMPLETE', '2024-01-20 14:15:00'),
(2, 3, 'CREDIT', 10000.00, 'COMPLETE', '2024-01-18 09:45:00'),
(3, 4, 'DEBIT', 7500.00, 'COMPLETE', '2024-01-22 16:20:00'),
(4, 5, 'DEBIT', 30000.00, 'COMPLETE', '2024-01-25 11:30:00'),
(1, 1, 'TRANSFER', 1000.00, 'PROCESSING', '2024-01-30 15:20:00');