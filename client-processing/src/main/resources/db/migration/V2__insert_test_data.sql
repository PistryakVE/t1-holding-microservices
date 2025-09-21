-- Вставка тестовых пользователей
INSERT INTO users (login, password, email) VALUES
('ivanov_ii', 'password123', 'ivanov@example.com'),
('petrov_pp', 'password456', 'petrov@example.com'),
('sidorova_ss', 'password789', 'sidorova@example.com'),
('smirnov_aa', 'password012', 'smirnov@example.com');

-- Вставка тестовых продуктов
INSERT INTO products (name, product_key, create_date, product_id) VALUES
('Дебетовая карта "Мир"', 'DC', '2024-01-15 10:00:00', 'DC1'),
('Кредитная карта "Золотая"', 'CC', '2024-01-15 11:00:00', 'CC2'),
('Вклад "Накопительный"', 'AC', '2024-01-15 12:00:00', 'AC3'),
('Ипотечный кредит', 'IPO', '2024-01-15 13:00:00', 'IPO4'),
('Пенсионный счет', 'PENS', '2024-01-15 14:00:00', 'PENS5'),
('Страхование жизни', 'INS', '2024-01-15 15:00:00', 'INS6');

-- Вставка тестовых клиентов
INSERT INTO clients (client_id, user_id, first_name, middle_name, last_name, date_of_birth, document_type, document_id, document_prefix, document_suffix) VALUES
('770100000001', 1, 'Иван', 'Иванович', 'Иванов', '1990-05-15', 'PASSPORT', '1234567890', '45', '123'),
('770100000002', 2, 'Петр', 'Петрович', 'Петров', '1985-08-22', 'PASSPORT', '0987654321', '46', '456'),
('770200000001', 3, 'Светлана', 'Сергеевна', 'Сидорова', '1992-12-03', 'PASSPORT', '1122334455', '47', '789'),
('770200000002', 4, 'Алексей', 'Александрович', 'Смирнов', '1988-03-18', 'PASSPORT', '5566778899', '48', '012');

-- Вставка связей клиентов с продуктами
INSERT INTO client_products (client_id, product_id, open_date, close_date, status) VALUES
(1, 1, '2024-01-20', NULL, 'ACTIVE'),
(1, 2, '2024-02-01', NULL, 'ACTIVE'),
(1, 3, '2024-01-25', NULL, 'ACTIVE'),
(2, 1, '2024-01-22', NULL, 'ACTIVE'),
(2, 4, '2024-02-05', NULL, 'ACTIVE'),
(3, 1, '2024-01-18', NULL, 'ACTIVE'),
(3, 5, '2024-02-10', NULL, 'ACTIVE'),
(3, 6, '2024-02-12', NULL, 'ACTIVE'),
(4, 1, '2024-01-30', NULL, 'ACTIVE'),
(4, 3, '2024-02-08', NULL, 'ACTIVE');